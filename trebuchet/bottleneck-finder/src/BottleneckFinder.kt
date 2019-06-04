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
 * Imports
 */

import java.io.File
import trebuchet.model.Model
import trebuchet.extras.parseTrace
import trebuchet.model.base.Slice
import trebuchet.queries.slices.SliceTraverser
import trebuchet.queries.slices.TraverseAction
import trebuchet.queries.slices.traverseSlices
import java.io.PrintStream

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

data class TreeNode(val id: Int, val parentId: Int, val depth: Int, val name: String, val durationMs: Double, val aggregated: Boolean = false)

// felipeal: rename or use pair
class ParentNode(val parent: TreeNode) {
    var otherDurationMs: Double = 0.0

    override fun toString() = "${parent.name}:${otherDurationMs}"
}

fun Model.findBottleneck(root: String, thresholdMs: Int = 0, otherName: String = "Other") : List<TreeNode> {
    // felipeal: move variables below to tranverSlices??
    val ret = arrayListOf<TreeNode>()
    val systemServerProc = this.findProcess(PROC_NAME_SYSTEM_SERVER)
    var started = false // felipeal: use parents.empty() instead?
    var finished = false
//    var otherDuration: Double = 0.0
    // felipeal: use Stack
    var stack = arrayListOf<ParentNode>()
    var nextId = 0

    systemServerProc.traverseSlices(object : SliceTraverser {
        var depth = 0;
        var parent : ParentNode? = null
//        var parent : TreeNode? = null
//        var child: TreeNode? = null // felipeal: remove
//        var childId = 0;

        override fun beginSlice(slice: Slice): TraverseAction {
            ++depth
            if (finished) return TraverseAction.DONE;

            val durationMs = slice.duration * 1000;
//            if (slice.name == root || started) println("BEGIN(${depth}): ${slice.name}: ${durationMs}") // felipeal
            if (slice.name == root) {
                println("STARTING ALL: " + durationMs + ">>> " + depth) // felipeal: tmp
                // Root node
                val node = TreeNode(++nextId, 0, depth, slice.name, durationMs)
                ret.add(node)
                val parentNode = ParentNode(node)
                stack.add(parentNode)
                parent = parentNode
                started = true
            } else if (!started) {
                // Node located outside root: ignore
                return TraverseAction.VISIT_CHILDREN
            } else {
                if (durationMs < thresholdMs) {
                    // Add to parent's "Other" and move on
                    parent!!.otherDurationMs += durationMs
//                    println("SKIPPING " + slice.name + ": " + durationMs) // felipeal
                    return TraverseAction.DONE
                } else {
//                    println("DUUUDE, CREATE PARENT NODE FOR " + slice.name + ": " + durationMs +" >> " + stack) // felipeal
//                    ++childId
//                    val nodeId = (parent != null) ? "${parent.id}.${childId}" : "${childId}";
//                    var parentId = parent?.parent?.id;
//                    val nodeId = "${parentId}.${childId}"
                    val node = TreeNode(++nextId, parent!!.parent!!.id, depth, slice.name, durationMs)
                    ret.add(node)
                    val parentNode = ParentNode(node)
                    stack.add(parentNode)
                    parent = parentNode
                }
            }
//            if (!finished) {
//                val durationMs = slice.duration * 1000;
//                if (durationMs > thresholdMs) {
//                    ++childId
//                    val nodeId = parent != null "${parent.id}.${childId}" ? "${childId}"
//                    val node = TreeNode("${nodeId}", parent?.id, depth, slice.name, slice.duration * 1000)
//                    // felipeal: remove
////                    val durationMs = String.format("%d-%s-%.2f", depth, slice.name, slice.duration * 1000)
//
//                    ret.add(node)
//                    child = node
//                } else {
//                    otherDuration += slice.duration
//                }
//            }
            return TraverseAction.VISIT_CHILDREN
        }

        override fun endSlice(slice: Slice) {
            depth--
            if (!started || stack.isEmpty()) return

//            // felipeal: stopped here: use parents.last instead (and remove Parent from other places)??
//            if (slice.name == "StartBatteryService") {
////                println("\n\n\nDUUUDE:" + parent!!.parent.name  +":\nEQ:" + (parent!!.parent.name.equals(slice.name)) + "\n==" + parent!!.parent.name == slice.name) // feliepal
//                println("\n\n\nDUUUDE:" + parent!!.parent.name + "\nEQ:" + ((parent!!.parent.name.equals(slice.name))) + "\n\n stack: " + stack)
//                return
//            }

//            if (started && ! finished) println("END SLICE: " + slice.name)
//            println("ADD OTHER DURATION: " + otherDuration + " FOR " + slice.name) // felipeal: tmp
            val last = stack.get(stack.size - 1)
            if (last!!.parent.name.equals(slice.name)) {
//                println("REMOVE " + slice.name + ">>> " + slice.duration + ">>> SIZE: " + stack.size + " >> " + stack) // felipeal: tmp
                val node = stack.removeAt(stack.size  - 1)
//                println("REMOVED " + slice.name + ">>> " + slice.duration + ">>> SIZE: " + stack.size + " >> " + stack) // felipeal: tmp
                parent = node
                if (node.otherDurationMs > 0) {
//                    println("DUDE, OTHER FOR " + slice.name + ">>> " + node.otherDurationMs + ">>> SIZE: " + parents.size) // felipeal: tmp
                    // felipeal: rever other name
                    val otherNode = TreeNode(++nextId, node.parent.id, node.parent.depth + 1, "${otherName}.${node.parent.name}", node.otherDurationMs)
                    ret.add(otherNode)
                }

//                if (!parents.isEmpty()) { // felipeal: really needed?
//                    println("ADD: " + slice.duration * 1000) // felipeal: tmp
//                }
            }
//            else println("IGNORE " + slice.name + ">>> " + slice.duration + ">>> SIZE: " + stack.size) // felipeal: tmp
            // felipeal: it's missing Other.startServices!!!

            // felipeal: remove below??
            if (slice.name.equals(root)) { // felipeal: removed?
                println("FINISHED: " + parent!!.parent) // felipeal
                finished = true; // felipeal: whre's used
            }
        }
    })

    // felipeal: here
//    if (otherDuration > 0) {
//        var otherSlice = AsyncSlice(name = otherName, startTime = 0.0, endTime = otherDuration, cookie = 0, didNotFinish = false, startThreadId = 0, endThreadId = 0)
//        ret.add(otherSlice)
//    }

    return ret

}
/*
 * Main Function
 */

fun main(args: Array<String>) {
    // felipeal: update
    if (args.isEmpty()) {
        println("Usage: SystemServerAnalyzerKt <trace_filename> [-t threshold_ms] [-o output_filename]")
        return
    }

    val input = args[0]

    println("Opening ${input}")
    val trace = parseTrace(File(input), verbose = true)

    var csvFormat = false
    var output = System.out
    var thresholdMs = 100;

    // Parse optional arguments
    var nextArg = 1
    while (nextArg < args.size) {
        var arg = args[nextArg++]
        var value = args[nextArg++]
        when (arg) {
            "-t" -> thresholdMs = value.toInt()
            "-o" -> {
                output = PrintStream(File(value).outputStream())
                csvFormat = true
                println("Writing CSV output to ${value}")
            }
            else -> println("invalid option: ${arg}")
        }
    }

//    measureServiceStartup(trace, "StartServices", thresholdMs, output, csvFormat)

//    measureSystemServerServices(trace, thresholdMs)

    // felipeal: get root from arguments
    val tree = trace.findBottleneck("StartServices", thresholdMs)

    // felipeal: not really working, Other... values don't match
    println("'Depth';Id;ParentId;Name;Duration")
    tree.forEach {
        // felipeal: agregrated
        val durationMs = String.format("%.2f", it.durationMs)
        println("'${it.depth}';'${it.id}';'${it.parentId}';'${it.name}';'${durationMs}'")
    }

}