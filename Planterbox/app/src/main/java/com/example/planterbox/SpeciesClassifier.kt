package com.example.planterbox

import android.content.Context
import android.graphics.Bitmap
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

class SpeciesClassifier(context: Context) {

    private val interpreter: Interpreter
    private val labels: List<String>
    private val inputSize = 224

    init {
        interpreter = Interpreter(loadModelFile(context, "model.tflite"))
        labels = loadLabels(context, "labels.txt")
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel: FileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadLabels(context: Context, filename: String): List<String> {
        val labels = mutableListOf<String>()
        context.assets.open(filename).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty()) labels.add(trimmed)
                }
            }
        }
        return labels
    }

    data class Prediction(
        val label: String,
        val confidence: Float
    )

    fun classify(bitmap: Bitmap, topK: Int = 3): List<Prediction> {
        val scaled = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Input shape: [1, 224, 224, 3] with raw 0–255 floats
        val input = Array(1) {
            Array(inputSize) {
                Array(inputSize) {
                    FloatArray(3)
                }
            }
        }

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val px = scaled.getPixel(x, y)

                // ✅ NO extra normalization here – just 0..255 floats
                val r = ((px shr 16) and 0xFF).toFloat()
                val g = ((px shr 8) and 0xFF).toFloat()
                val b = (px and 0xFF).toFloat()

                input[0][y][x][0] = r
                input[0][y][x][1] = g
                input[0][y][x][2] = b
            }
        }

        val numClasses = labels.size
        val output = Array(1) { FloatArray(numClasses) }
        interpreter.run(input, output)

// TFLite output already has softmax applied, so these are probabilities
        val probs = output[0].toList()

        val predictions = labels.zip(probs).map { (label, p) ->
            Prediction(label = label, confidence = p.coerceIn(0f, 1f))
        }.sortedByDescending { it.confidence }

        return predictions.take(topK)

    }



}
