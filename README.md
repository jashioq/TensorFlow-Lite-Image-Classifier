# Offline image classification app

A simple Android app that classifies images using TensorFlow Lite and MobileNetV2. Everything runs on-device with no internet required.

https://github.com/user-attachments/assets/aef02b85-367e-4b89-9e56-22fa54f19a64

## How it works

This app transforms images into AI predictions through a 4-step pipeline:

---

### **Step 1: Image preprocessing**

The raw image is converted to the format MobileNetV2 expects:

```kotlin
// Resize to 224x224
val scaledBitmap = bitmap.scale(INPUT_SIZE, INPUT_SIZE)

// Load into TensorImage
var tensorImage = TensorImage(DataType.UINT8)
tensorImage.load(scaledBitmap)

// Normalize pixels from [0, 255] to [-1, 1]
val imageProcessor = ImageProcessor.Builder()
    .add(NormalizeOp(127.5f, 127.5f))
    .build()
tensorImage = imageProcessor.process(tensorImage)
```

The normalization formula transforms each pixel:

```math
\text{normalized} = \frac{\text{pixel} - 127.5}{127.5}
```

This centers the data around zero, which helps the neural network process it efficiently.

---

### **Step 2: Neural network processing**

The preprocessed tensor is fed through MobileNetV2:

```kotlin
val output = Array(1) { FloatArray(1000) }
interpreter.run(tensorImage.buffer, output)
```

**What happens inside:**
- Input: 224×224×3 normalized tensor
- Architecture: MobileNetV2 (53 layers, 3.4M parameters)
- Output: 1000 probabilities, one for each ImageNet class

The network extracts features through its layers and outputs a probability distribution where all values sum to 1.

---

### **Step 3: Results selection**

Sort predictions by confidence and take the top 5:

```kotlin
val results = output[0]
    .mapIndexed { index, confidence ->
        ClassificationResult(
            label = labels[index],
            confidence = confidence
        )
    }
    .sortedByDescending { it.confidence }
    .take(5)
```

Example output:
```
golden retriever: 89%
Labrador retriever: 5%
cocker spaniel: 3%
```

---

### **Step 4: UI display**

Results are shown in a Material Design 3 bottom sheet with:
- Image preview
- Processing time in milliseconds
- Top 5 predictions with confidence bars

The app uses MVVM architecture with Jetpack Compose for reactive UI updates:

```kotlin
sealed class ClassificationState {
    data object Idle
    data object Selecting
    data object Processing
    data class ShowingResults(
        val result: List<ClassificationResult>,
        val bitmap: Bitmap,
        val inferenceTime: Long
    )
}
```

Classification runs on a background thread to keep the UI responsive.
