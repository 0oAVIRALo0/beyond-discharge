package com.example.frontend

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.frontend.ui.theme.MyApplicationTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MyApplicationTheme {
                AppNavigation()
            }
        }
    }
}

// Main App Navigation
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("main") }

    when (currentScreen) {
        "main" -> MainScreen(onNavigate = { screen -> currentScreen = screen })
        "fhir" -> FHIRScreen(onBack = { currentScreen = "main" })
        "camera" -> CameraOCRScreen(onBack = { currentScreen = "main" })
    }
}

// Main Screen
@Composable
fun MainScreen(onNavigate: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { onNavigate("fhir") }) {
            Text("Use hospital's FHIR Server")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { onNavigate("camera") }) {
            Text("Scan a new document")
        }
    }
}

// FHIR Server Screen
@Composable
fun FHIRScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var dischargeSummary by remember { mutableStateOf<String?>(null) }
    var predictionResult by remember { mutableStateOf<String?>(null) }
    var patient_id by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Added vertical scrolling
        verticalArrangement = Arrangement.Top, // Changed to top to align content at the start
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Text input for patient ID
        TextField(
            value = patient_id,
            onValueChange = { patient_id = it },
            label = { Text("Enter Patient ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            if (patient_id.isNotBlank()) {
                fetchDischargeSummary(context, patient_id) { summary ->
                    dischargeSummary = summary.toString()
                }
            } else {
                Toast.makeText(context, "Please enter a valid Patient ID", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Fetch Discharge Summary")
        }

        Spacer(modifier = Modifier.height(16.dp))

        dischargeSummary?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                sendPrediction(context, it) { result ->
                    predictionResult = result
                }
            }) {
                Text("Predict")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Log.d("Prediction", "${predictionResult}")
            predictionResult?.let { result ->
                Text(
                    text = result,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { onBack() }) {
            Text("Back")
        }
    }
}


@Composable
fun CameraOCRApp() {
    val context = LocalContext.current
    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
    var recognizedText by remember { mutableStateOf<String?>(null) }

    val captureImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            capturedImageUri = null
            Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
        }
    }

    val fileProviderAuthority = "${context.packageName}.provider"

    // Helper to get URI for saving the image
    fun getImageUri(): Uri? {
        val tempFile = File(context.cacheDir, "temp_image.jpg")
        return FileProvider.getUriForFile(context, fileProviderAuthority, tempFile)
    }


    // Perform OCR using Google ML Kit
    fun performOCR(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(context, "No image to perform OCR", Toast.LENGTH_SHORT).show()
            return
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    recognizedText = visionText.text // Update the recognized text state
                }
                .addOnFailureListener { e ->
                    Log.e("OCR", "Error recognizing text", e)
                    Toast.makeText(context, "Error recognizing text", Toast.LENGTH_SHORT).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        capturedImageUri?.let { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Captured Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        recognizedText?.let { text ->
            Text(
                text = text, // Display the recognized text
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } ?: Text( // Fallback text when no recognized text is available
            text = "No text recognized yet.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val uri = getImageUri()
            if (uri != null) {
                capturedImageUri = uri
                captureImageLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Unable to create file for image capture", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("Capture Image")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { performOCR(capturedImageUri) }) {
            Text("Perform OCR")
        }
        // Button to trigger the prediction using the recognized text from OCR
        Button(onClick = {
            recognizedText?.let { text ->
                sendPrediction(context, text) { prediction ->
                    // Handle the prediction result
                    if (prediction != null) {
                        // Show prediction result (e.g., update the UI or show a toast)
                        Toast.makeText(context, "Prediction: $prediction", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Prediction failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: Toast.makeText(context, "No text recognized for prediction", Toast.LENGTH_SHORT).show()
        }) {
            Text("Predict")
        }
    }
}


// Camera OCR Screen
@Composable
fun CameraOCRScreen(onBack: () -> Unit) {
    // Reuse your existing CameraOCRApp composable here
    CameraOCRApp()

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = { onBack() }) {
        Text("Back")
    }
}

// Modify the call to pass the request body
fun fetchDischargeSummary(context: Context, patientId: String, onResult: (Any?) -> Unit) {
    val apiService = ApiClient.apiService
    val request = DischargeSummaryRequest(patientId)  // Send the request body with patientId
    apiService.getDischargeSummary(request).enqueue(object : retrofit2.Callback<DischargeSummaryResponse> {
        override fun onResponse(call: Call<DischargeSummaryResponse>, response: retrofit2.Response<DischargeSummaryResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    // Check if the discharge_summaries is a list or a string
                    if (responseBody.discharge_summaries is List<*>) {
                        // If it's a list, handle as usual
                        Log.d("FetchDischargeSummary", "Discharge Summaries: ${responseBody.discharge_summaries}")
                        onResult(responseBody.discharge_summaries)
                    } else if (responseBody.discharge_summaries is String) {
                        // Handle the case where it's a string (could be an error or message)
                        Log.e("FetchDischargeSummary", "Received a string instead of an array: ${responseBody.discharge_summaries}")
//                        Toast.makeText(context, "Received unexpected data format: ${responseBody.discharge_summaries}", Toast.LENGTH_SHORT).show()
                        onResult(responseBody.discharge_summaries)
                    }
                } else {
                    Log.w("FetchDischargeSummary", "Response body is null")
                    Toast.makeText(context, "No discharge summary available", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            } else {
                Log.e("FetchDischargeSummary", "Failed response: ${response.message()}")
                Toast.makeText(context, "Failed to fetch discharge summary: ${response.message()}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }

        override fun onFailure(call: Call<DischargeSummaryResponse>, t: Throwable) {
            Log.e("FetchDischargeSummary", "Error: ${t.message}", t)
            Toast.makeText(context, "Unable to reach server: ${t.message}", Toast.LENGTH_SHORT).show()
            onResult(null)
        }
    })
}



data class DischargeSummaryResponse(
    val discharge_summaries: Any // This can be either a String or a List
)

data class DischargeSummaryRequest(
    val patient_id: String
)

// Prediction
private fun sendPrediction(context: Context, dischargeSummary: String, onResult: (String?) -> Unit) {
    val apiService = ApiClient.apiService
    val textRequest = predictionRequest(dischargeSummary)

    // Send the request to the server and handle the response
    apiService.sendText(textRequest).enqueue(object : retrofit2.Callback<predictionResponse> {
        override fun onResponse(call: Call<predictionResponse>, response: retrofit2.Response<predictionResponse>) {
            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.e("bhang bhosda",responseBody.toString())
                if (responseBody != null) {
                    // Check if the response contains a prediction (as a String)
                    val prediction = responseBody.prediction
                    if (prediction != null) {
                        // Handle the prediction (which should be a String)
                        Log.d("SendPrediction", "Prediction: $prediction")
                        onResult(prediction)
                    } else {
                        // Handle the case where prediction is null
                        Log.e("SendPrediction", "Prediction is null")
                        Toast.makeText(context, "Prediction result is empty", Toast.LENGTH_SHORT).show()
                        onResult(null)
                    }
                } else {
                    Log.w("SendPrediction", "Response body is null")
                    Toast.makeText(context, "Prediction result is empty", Toast.LENGTH_SHORT).show()
                    onResult(null)
                }
            } else {
                // Log the error response details
                Log.e("SendPrediction", "Error Response Code: ${response.code()}")
                Log.e("SendPrediction", "Error Message: ${response.message()}")
                Toast.makeText(context, "Failed to get prediction: ${response.message()}", Toast.LENGTH_SHORT).show()
                onResult(null)
            }
        }

        override fun onFailure(call: Call<predictionResponse>, t: Throwable) {
            // Log failure details
            Log.e("SendPrediction", "Error: ${t.message}", t)

            // In case of failure (e.g., network issues)
            Toast.makeText(context, "Unable to reach server: ${t.message}", Toast.LENGTH_SHORT).show()
            onResult(null)
        }
    })
}

// Data classes for Retrofit
data class predictionRequest(val input: String)

data class predictionResponse(val prediction: String?) // Changed to match the backend response


interface ApiService {
    @POST("/fetchDischarge")
    fun getDischargeSummary(@Body request: DischargeSummaryRequest): Call<DischargeSummaryResponse>

    @POST("/getPrediction")
    fun sendText(@Body textRequest: predictionRequest): Call<predictionResponse>
}

object ApiClient {
    private const val BASE_URL = "http://192.168.113.18:5001"

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen(onNavigate = {})
    }
}