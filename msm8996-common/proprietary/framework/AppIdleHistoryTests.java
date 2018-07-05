
/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.usage;

import android.os.FileUtils;
import android.test.AndroidTestCase;

import java.io.File;

public class AppIdleHistoryTests extends AndroidTestCase {

    File mStorageDir;

    final static String PACKAGE_1 = "com.android.testpackage1";
    final static String PACKAGE_2 = "com.android.testpackage2";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mStorageDir = new File(getContext().getFilesDir(), "appidle");
        mStorageDir.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteContents(mStorageDir);
        super.tearDown();
    }

    public void testFilesCreation() {
        final int userId = 0;
        AppIdleHistory aih = new AppIdleHistory(mStorageDir, 0);

        aih.updateDisplay(true, /* elapsedRealtime= */ 100);
        aih.updateDisplay(false, /* elapsedRealtime= */ 200);
        // Screen On time file should be written right away
        assertTrue(aih.getScreenOnTimeFile().exists());

        aih.writeAppIdleTimes(userId);
        // stats file should be written now
        assertTrue(new File(new File(mStorageDir, "users/" + userId),
                AppIdleHistory.APP_IDLE_FILENAME).exists());
    }

    public void testScreenOnTime() {
        AppIdleHistory aih = new AppIdleHistory(mStorageDir, 100);
        aih.updateDisplay(false, 200);
        assertEquals(aih.getScreenOnTime(200), 0);
        aih.updateDisplay(true, 300);
        assertEquals(aih.getScreenOnTime(400), 100);
        assertEquals(aih.getScreenOnTime(500), 200);
        aih.updateDisplay(false, 600);
        // Screen on time should not keep progressing with screen is off
        assertEquals(aih.getScreenOnTime(700), 300);
        assertEquals(aih.getScreenOnTime(800), 300);
        aih.writeAppIdleDurations();

        // Check if the screen on time is persisted across instantiations
        AppIdleHistory aih2 = new AppIdleHistory(mStorageDir, 0);
        assertEquals(aih2.getScreenOnTime(1100), 300);
        aih2.updateDisplay(true, 400);
        aih2.updateDisplay(false, 500);
        assertEquals(aih2.getScreenOnTime(1300), 400);
    }

    public void testPackageEvents() {
        AppIdleHistory aih = new AppIdleHistory(mStorageDir, 100);
        aih.setThresholds(400, 100);
        aih.updateDisplay(true, 100);
        // App is not-idle by default
        assertFalse(aih.isIdle(PACKAGE_1, 0, 150));
        // Still not idle
        assertFalse(aih.isIdle(PACKAGE_1, 0, 300));
        // Idle now
        assertTrue(aih.isIdle(PACKAGE_1, 0, 800));
        // Not idle
        assertFalse(aih.isIdle(PACKAGE_2, 0, 900));

        // Screen off
        aih.updateDisplay(false, 9100);
        // Still idle after 10 seconds because screen hasn't been on long enough
        assertFalse(aih.isIdle(PACKAGE_2, 0, 2000));
        aih.updateDisplay(true, 21000);
        assertTrue(aih.isIdle(PACKAGE_2, 0, 2300));
    }
}