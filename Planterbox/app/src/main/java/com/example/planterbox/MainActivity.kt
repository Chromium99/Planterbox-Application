package com.example.planterbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.planterbox.ui.theme.PlanterboxTheme
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.planterbox.net.BackendClient
import com.example.planterbox.net.Diagnostic
import com.example.planterbox.net.ProcessApi
import com.example.planterbox.net.bitmapToImagePart
import com.example.planterbox.net.textPart

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlanterboxTheme {
                val navController = rememberNavController()
                PlanterboxNavHost(navController)
            }
        }
    }
}

/** -------- NAVIGATION SETUP -------- */

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"      // NEW: plant identifier screen
    const val SCAN = "scan"
    const val RESULT = "result"
}

@Composable
fun PlanterboxNavHost(navController: NavHostController) {
    // 🔹 Shared URI for gallery-selected image
    var galleryImageUri by remember { mutableStateOf<Uri?>(null) }

    // 🔹 Shared state for analysis result
    var resultLabel by remember { mutableStateOf<String?>(null) }
    var resultConfidence by remember { mutableStateOf<Float?>(null) }
    var resultDiagnostic by remember { mutableStateOf<Diagnostic?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.ONBOARDING
        ) {
            composable(Routes.ONBOARDING) {
                GardenOnboardingScreen(
                    onGetStarted = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                IdentifierScreen(
                    onScanClick = { navController.navigate(Routes.SCAN) },
                    onImageSelected = { uri ->
                        galleryImageUri = uri
                        navController.navigate(Routes.SCAN)
                    }
                )
            }
            composable(Routes.SCAN) {
                ScanScreen(
                    initialImageUri = galleryImageUri,
                    onAnalysisCompleted = { label, confidence, diagnostic ->
                        resultLabel = label
                        resultConfidence = confidence
                        resultDiagnostic = diagnostic
                        navController.navigate(Routes.RESULT)
                    }
                )

            }
            composable(Routes.RESULT) {
                PlantResultScreen(
                    label = resultLabel,
                    confidence = resultConfidence,
                    diagnostic = resultDiagnostic,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}




/** -------- SCREEN 1: ONBOARDING -------- */

@Composable
fun GardenOnboardingScreen(onGetStarted: () -> Unit) {
    // Soft green gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9), // light mint
                        Color(0xFFC8E6C9)  // soft green
                    )
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        // Center card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Round logo with sprout
                Surface(
                    modifier = Modifier.size(96.dp),
                    shape = CircleShape,
                    color = Color(0xFF66BB6A).copy(alpha = 0.18f),
                    border = BorderStroke(2.dp, Color(0xFF66BB6A))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🌱",
                            fontSize = 36.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Planterbox",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Identify plants instantly,\nkeep your garden thriving.",
                    fontSize = 14.sp,
                    color = Color(0xFF4E6A52),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Main button → go to Scan screen
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF66BB6A),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun IdentifierScreen(
    onScanClick: () -> Unit = {},
    onImageSelected: (Uri) -> Unit = {}
) {
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            onImageSelected(uri)   // 🔹 notify parent
        } else {
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    // permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            galleryLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun openGalleryWithPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            galleryLauncher.launch("image/*")
        } else {
            permissionLauncher.launch(permission)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9)
                    )
                )
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Plant Identifier",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Upload button – opens gallery with permission
                OutlinedButton(
                    onClick = { openGalleryWithPermission() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, Color(0xFF444444))
                ) {
                    Text(text = "Upload Image", fontSize = 15.sp, color = Color(0xFF444444))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scan button – goes to camera screen
                OutlinedButton(
                    onClick = onScanClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(2.dp, Color(0xFF1B5E20))
                ) {
                    Text(
                        text = "Scan Plant",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Optional: tiny status text when an image is chosen
                if (selectedImageUri != null) {
                    Text(
                        text = "Image selected from gallery ✔",
                        fontSize = 12.sp,
                        color = Color(0xFF4E6A52)
                    )
                }
            }
        }
    }
}


/** -------- SCREEN 2: SCAN / HOME PLACEHOLDER -------- */

@Composable
fun ScanScreen(
    initialImageUri: Uri? = null,
    onAnalysisCompleted: (String, Float, Diagnostic?) -> Unit
) {
    val context = LocalContext.current

    val detectionThreshold = 0.55f  // 55%
    val scope = rememberCoroutineScope()

    // 🔹 Classifier + error state
    var classifier by remember { mutableStateOf<SpeciesClassifier?>(null) }
    var classifierError by remember { mutableStateOf<String?>(null) }

    // Load TFLite model once, safely
    LaunchedEffect(Unit) {
        try {
            classifier = SpeciesClassifier(context)
            classifierError = null
        } catch (e: Exception) {
            classifierError = "Error loading model: ${e.message}"
        }
    }

    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }


    // 📷 Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedImage = bitmap
            errorText = null
        } else {
            errorText = "No image captured."
        }
    }

    // 💾 Load gallery image if we navigated here from "Upload Image"
    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null && capturedImage == null) {
            try {
                val input = context.contentResolver.openInputStream(initialImageUri)
                val bmp = input?.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    capturedImage = bmp
                    errorText = null
                } else {
                    errorText = "Could not load selected image."
                }
            } catch (e: Exception) {
                errorText = "Error loading image: ${e.message}"
            }
        }
    }

    // ---------- UI ----------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9)
                    )
                )
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Image card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(2.dp, Color(0xFFB0B0B0))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (capturedImage != null) {
                        Image(
                            bitmap = capturedImage!!.asImageBitmap(),
                            contentDescription = "Plant photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "IMAGE PLACEHOLDER",
                                fontSize = 14.sp,
                                color = Color(0xFF888888)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Photo will appear here",
                                fontSize = 12.sp,
                                color = Color(0xFFAAAAAA)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 👉 Take / Analyze button
            OutlinedButton(
                onClick = {
                    if (capturedImage == null) {
                        // Step 1: Take photo
                        cameraLauncher.launch(null)
                    } else {
                        // Step 2: Run the model
                        val cls = classifier
                        if (cls == null) {
                            errorText = classifierError ?: "Model not ready yet."
                        } else {
                            try {
                                val preds = cls.classify(capturedImage!!, topK = 3)
                                val best = preds.maxByOrNull { it.confidence }

                                if (best == null || best.confidence < detectionThreshold) {
                                    errorText = "PLANT NOT DETECTED. Please retake."
                                } else {
                                    errorText = null
                                    isAnalyzing = true

                                    scope.launch {
                                        try {
                                            val api = BackendClient.retrofit.create(ProcessApi::class.java)
                                            val resp = api.processImage(
                                                image = bitmapToImagePart(capturedImage!!),
                                                speciesName = textPart(best.label)
                                            )

                                            isAnalyzing = false
                                            onAnalysisCompleted(best.label, best.confidence, resp.data?.diagnostic)
                                        } catch (e: Exception) {
                                            isAnalyzing = false
                                            errorText = "AI analysis failed: ${e.message}"
                                        }
                                    }
                                }

                            } catch (e: Exception) {
                                errorText = "Error running model: ${e.message}"
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Color(0xFF1B5E20))
            ) {
                Text(
                    text = if (capturedImage == null) "Take Photo" else "Analyze Plant",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🔁 Retake button
            OutlinedButton(
                onClick = {
                    capturedImage = null
                    errorText = null
                    cameraLauncher.launch(null)
                },
                enabled = capturedImage != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(2.dp, Color(0xFF444444))
            ) {
                Text(
                    text = "Retake Photo",
                    fontSize = 15.sp,
                    color = if (capturedImage != null) Color(0xFF444444) else Color(0xFFBBBBBB)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(6.dp))
                Text("Analyzing with AI...", fontSize = 12.sp, color = Color(0xFF4E6A52))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ⚠️ Only show errors (no prediction text on this screen)
            val combinedError = errorText ?: classifierError
            if (combinedError != null) {
                Text(
                    text = combinedError,
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
fun PlantResultScreen(
    label: String?,
    confidence: Float?,
    diagnostic: Diagnostic?,
    onBack: () -> Unit
) {
    val name = label ?: "Unknown plant"
    val confPercent = confidence?.let { String.format("%.1f", it * 100f) }

    // ✅ Use LLM output from backend (fallbacks included)
    val tipsText = diagnostic?.care_recommendations
        ?: diagnostic?.error
        ?: "No AI care tips received. Please try again."

    val urgency = diagnostic?.urgency_level

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE8F5E9),
                        Color(0xFFC8E6C9)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = name,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            if (confPercent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Confidence: $confPercent%",
                    fontSize = 14.sp,
                    color = Color(0xFF4E6A52)
                )
            }

            if (!urgency.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Urgency: ${urgency.uppercase()}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Care tips (LLM)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Care Tips",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = tipsText,
                        fontSize = 14.sp,
                        color = Color(0xFF4E6A52),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, Color(0xFF1B5E20))
            ) {
                Text(
                    text = "Back to Scanner",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

/** -------- PREVIEWS -------- */

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingPreview() {
    PlanterboxTheme {
        GardenOnboardingScreen(onGetStarted = {})
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ScanPreview() {
    PlanterboxTheme {
        ScanScreen(onAnalysisCompleted = { _, _, _ -> })
    }
}

