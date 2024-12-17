# beyond-discharge

This Android application predicts ICU readmissions using discharge summaries. The application supports two methods for obtaining discharge summaries:
1. **FHIR Server Integration**: Fetch discharge summaries directly from a hospital's FHIR server.
2. **OCR Integration**: Capture a discharge summary document using the device camera, extract the text using OCR, and predict ICU readmission.

The backend server processes discharge summaries and predicts ICU readmission probabilities using a pre-trained machine learning model.


## Features

### Frontend (Android App)
- **Main Screen**:
  - Navigate to either FHIR Server or OCR-based prediction.
- **FHIR Integration**:
  - Enter the patient ID to fetch the discharge summary from the hospital's FHIR server.
  - Predict ICU readmissions based on the retrieved discharge summary.
- **OCR Integration**:
  - Capture an image of the discharge summary.
  - Perform text recognition using Google ML Kit's OCR.
  - Predict ICU readmissions based on the extracted text.

### Backend (Flask Server)
- **FHIR Server Integration**:
  - Fetch discharge summaries using the FHIR API.
- **Prediction Model**:
  - A pre-trained machine learning model (`Linear SVC`) is used to predict ICU readmissions.
  - The model uses `TF-IDF` vectorization to process the text data.
- **Text Cleaning**:
  - Text is cleaned by removing punctuation, extra spaces, and normalizing case before prediction.


## Getting Started

### Prerequisites
- **Frontend**:
  - Android Studio installed on your system.
  - Device or emulator with internet access for testing the app.
- **Backend**:
  - Python 3.8+
  - Required Python libraries (`Flask`, `fhirpy`, `pickle`, `scikit-learn`, etc.).
