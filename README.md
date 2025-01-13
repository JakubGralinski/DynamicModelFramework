# 🌟 DynamicModelFramework 🌟

DynamicModelFramework is a **Java-based application** designed to model and simulate **economic indices** dynamically using JSON input data. The project provides a **flexible and interactive GUI**, allowing users to run, analyze, and visualize results across various predefined and custom models.

---

## 🚀 Key Features

### 🎛️ **Model Framework GUI**
- **🌟 Interactive GUI**: Provides an intuitive interface for loading data, running models, and visualizing results.
- **🔄 Dynamic Model Selection**: Choose between `Model1`, `Model2`, and `Model3` to suit your simulation needs.

### 📊 **Dynamic Modeling**
- **🟢 Model1**: Basic GDP calculation model.
- **🔵 Model2**: Adds **net exports** as a key metric.
- **🔴 Model3**: Introduces **randomness** with a **shock factor** for advanced simulations.

### 🛠️ **Annotation-Driven Data Binding**
- **🔗 @Bind Annotation**: Connects JSON keys to model variables seamlessly.
- **🔍 Reflection Mechanism**: Dynamically sets fields in the model based on input data.

### 📈 **Advanced Data Visualization**
- Generate **beautiful plots** for key indices, including:
  - 📊 Large-number indices (e.g., GDP, net exports).
  - 📉 Small-number indices (e.g., transformation weights).
  - 📊 Export-to-GDP ratio (`ZDEKS`).

### 🐍 **Python Integration**
- Leverages Python's **Jupyter notebooks** (`nbconvert`) for additional data processing and visualization.

### 📂 **Output Handling**
- **💾 Save results** in JSON and TSV formats.
- **📁 Export plots** for detailed analysis.

---

## 🛠️ How It Works

### 🔄 **Data Flow**
1. **📥 Load JSON Data**: Import data into the system via the GUI.
2. **🔗 Set Data**: Use the `setData` method to bind input JSON fields to model variables using annotations.
3. **▶️ Run Model**: Execute calculations via the selected model.
4. **📤 Generate Results**: Save results in JSON and TSV formats.
5. **📊 Visualize**: View detailed plots generated dynamically.

### 🧩 **Core Components**
- **🔗 Annotations (`@Bind`)**: Bridges JSON keys and Java model variables.
- **🔍 Reflection**: Dynamically maps input data to annotated fields.
- **🔄 Extrapolation**: Ensures all arrays have consistent lengths, filling missing values with the last known value.

---

## 📖 How to Use

1️⃣ Clone the repository:  
   ```bash
   git clone https://github.com/YourUsername/DynamicModelFramework.git

2️⃣ Navigate to the project directory:

cd DynamicModelFramework

3️⃣ Build the project:

If using Maven, run:

mvn clean install

4️⃣ Run the GUI application:

java -jar target/DynamicModelFramework.jar

5️⃣ Load your JSON data:
	•	Open the GUI.
	•	Click Load Data and select your JSON file.

6️⃣ Select a model:
	•	Use the dropdown to choose between Model1, Model2, or Model3.

7️⃣ Run the model:
	•	Click Run Model to execute the simulation.

8️⃣ View results:
	•	View calculated indices in the table.
	•	Save them in TSV or JSON format.
	•	Visualize results in the Plots Section.

🧰 Dependencies
	•	☕ Java: Version 11 or higher.
	•	🛠️ Gson: For JSON parsing.
	•	📉 Matplotlib (via Python): For generating plots.
	•	🖼️ Swing: For GUI development.

📂 Repository Structure

DynamicModelFramework/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── models/         # Model definitions (Model1, Model2, Model3)
│   │   │   ├── org/example/    # GUI, controller, and annotations
│   │   ├── resources/          # Jupyter notebooks, sample JSON files, and plots
├── README.md                   # Project documentation

🤝 Contribution

Contributions are welcome! 🎉 Feel free to fork this repository and submit a pull request.
	1.	Fork the repository. 🍴
	2.	Create a new branch. 🌿
	3.	Commit your changes. 💾
	4.	Submit a pull request. 🚀

📜 License

This project is licensed under the MIT License. See LICENSE for details. 📄

🎉 Happy Coding! 💻 🚀

