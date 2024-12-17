from flask import Flask, request, jsonify
import pickle
from fhirpy import SyncFHIRClient
from fhirpy import SyncFHIRClient
import string

# Initialize the FHIR client
client = SyncFHIRClient('https://server.fire.ly')
import base64
app = Flask(__name__)

with open('linear_svc_model.pkl', 'rb') as file:
    loaded_model = pickle.load(file)

# Assuming tfidf_vectorizer was also saved
with open('tfidf_vectorizer.pkl', 'rb') as file:
    loaded_vectorizer = pickle.load(file)
def clean_text(text):
    """
    Cleans the input text by:
    - Removing '' and '\n'
    - Converting text to lowercase
    - Removing punctuation
    - Removing extra spaces

    Args:
    text (str): The input text to clean.

    Returns:
    str: The cleaned text.
    """
    # Replace '' and '\n' with space, then convert to lowercase
    cleaned_text = text.replace('', '').replace('\n', ' ').lower()
    
    # Remove punctuation
    cleaned_text = cleaned_text.translate(str.maketrans('', '', string.punctuation))
    
    # Remove extra spaces
    cleaned_text = ' '.join(cleaned_text.split())
    
    return cleaned_text
# Function to predict the label for a single text input
def predict_single_text(text):
    text=clean_text(text)
    # Preprocess the input text
    text_vectorized = loaded_vectorizer.transform([text]).toarray()
    
    # Make prediction
    prediction = loaded_model.predict(text_vectorized)
    if(prediction==1):
        return "YES"
    return "NO"


def retrieve_discharge_summary(patient_id):
    print("Retrieving discharge summary for the patient")
    search_results = client.resources('DocumentReference').search(
        subject=f'Patient/{patient_id}',
        type="http://loinc.org|18842-5"
    ).limit(1).fetch()
    if search_results:
        discharge_summary = search_results[0]
        # Decoding the base64 data
        encoded_data = discharge_summary['content'][0]['attachment']['data']
        decoded_data = base64.b64decode(encoded_data).decode('utf-8')
        # print(f"Discharge Summary Content: {decoded_data}")
        return decoded_data
    else:
        print(f"No Discharge Summary found for patient {patient_id}")
def predict(input_string):
    # Use the loaded model to make a prediction
    try:
        # Reshape input to be compatible with the model
        return predict_single_text(input_string)  # Return the first prediction
    except Exception as e:
        return str(e)  # Handle errors gracefully

@app.route('/getPrediction', methods=['POST'])
def get_prediction():
    try:
        data = request.get_json()
        if not data or 'input' not in data:
            return jsonify({"error": "Invalid input format. Expected a JSON with an 'input' key."}), 400

        input_string = data['input']

        # Call the prediction function
        result = predict(input_string)
        print(f"server {result}")
        return jsonify({"prediction": result}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/fetchDischarge', methods=['POST'])
def fetch_discharge():
    try:
        # Log the incoming request
        data = request.get_json()
        if not data or 'patient_id' not in data:
            return jsonify({"error": "Invalid input format. Expected a JSON with a 'patient_id' key."}), 400

        patient_id = data['patient_id']

        # Log the request body and response before returning
        print(f"Request received for patient_id: {patient_id}")

        # Assuming you fetch the discharge summary (this part should match your actual logic)
        discharge_summaries = retrieve_discharge_summary(patient_id)
        
        # Log the discharge summaries response for debugging
        # print(f"Discharge Summaries: {discharge_summaries}")

        return jsonify({"discharge_summaries": discharge_summaries}), 200

    except Exception as e:
        return jsonify({"error": str(e)}),500

if __name__ == '__main__':
    app.run(debug=True, host="0.0.0.0", port=5001)
