/*
 * Copyright 2019 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Notes
 *
 * TODO (felipeal): generate .csv file
 * TODO (felipeal): automatically generate atrace / restart system_server
 */

/*
 * Imports
 */

import java.io.File
import trebuchet.model.Model
import trebuchet.extras.parseTrace
import trebuchet.util.time.*

/*
 * Constants
 */

/*
 * Class Definition
 */

/*
 * Class Extensions
 */

/*
 * Helper Functions
 */

fun measureSystemServerServices(model: Model) {

    val slices = model.getEventAndChildren("StartServices", 5);
    slices.forEach() {
        val durationMs = String.format("%.2f", it.duration * 1000)
        println("${it.name}: ${durationMs}ms");
    }
}

/*
 * Main Function
 */

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: SystemServerAnalyzerKt <trace filename>")
        return
    }

    val filename = args[0]

    println("Opening `$filename`")
    val trace = parseTrace(File(filename), verbose = true)

    println()
    measureSystemServerServices(trace)
}