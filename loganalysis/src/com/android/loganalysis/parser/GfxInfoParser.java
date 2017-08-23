/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.loganalysis.parser;

import com.android.loganalysis.item.GfxInfoItem;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link IParser} to handle the output of {@code gfxinfo}.
 */
public class GfxInfoParser implements IParser {

    // Example: "** Graphics info for pid 853 [com.google.android.leanbacklauncher] **"
    private static final Pattern PID_PREFIX = Pattern.compile(
            "\\*\\* Graphics info for pid (\\d+) \\[(.+)\\] \\*\\*");

    // Example: "Total frames rendered: 20391"
    private static final Pattern TOTAL_FRAMES_PREFIX = Pattern.compile(
            "Total frames rendered: (\\d+)");

    // Example: "Janky frames: 785 (3.85%)"
    private static final Pattern JANKY_FRAMES_PREFIX = Pattern.compile(
            "Janky frames: (\\d+) \\(.+\\%\\)");

    /**
     * Parses the log of "dumpsys gfxinfo".
     * Currently it only parses total frame number and total jank number per process.
     * This method only works for M and later.
     */
    @Override
    public GfxInfoItem parse(List<String> lines) {
        GfxInfoItem item = new GfxInfoItem();

        String name = null;
        Integer pid = null;
        Long totalFrames = null;
        Long jankyFrames = null;

        // gfxinfo also offers stats for specific views, but this parser
        // only records per process data. See example in GfxInfoParserTest.java.

        for (String line : lines) {
            Matcher m = PID_PREFIX.matcher(line);
            if (m.matches() && m.groupCount() == 2) {
                // New process line, clear data.
                pid = Integer.parseInt(m.group(1));
                name = m.group(2);

                totalFrames = null;
                jankyFrames = null;
            }

            m = TOTAL_FRAMES_PREFIX.matcher(line);
            if (totalFrames == null && m.matches()) {
                totalFrames = Long.parseLong(m.group(1));
            }

            m = JANKY_FRAMES_PREFIX.matcher(line);
            if (jankyFrames == null && m.matches()) {
                jankyFrames = Long.parseLong(m.group(1));
            }

            if (name != null && pid != null && totalFrames != null && jankyFrames != null) {
                // All the data for the process is recorded, add as a row.
                item.addRow(pid, name, totalFrames, jankyFrames);

                name = null;
                pid = null;
                totalFrames = null;
                jankyFrames = null;
            }
        }

        return item;
    }
}
