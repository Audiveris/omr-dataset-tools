# Inference

---

## Tests

The final .pt model was exported to ONNX format to ease its use by a Java program which uses
the OnnxRuntimeRunner of DeepLearning4j.

We could thus easily run the model on some of the scores processed by Audiveris.

Notes:
- The `staff` label is generally not reliable (much shorter than reality)
and of no great interest for Audiveris.
- The `stem` label is rarely detected and is often much higher than reality.
- No `acciaccatura` and no `appoggiatura` instance in the train set.
- No `fingering` (0,1,2,3,4,5), no `plucking` (p, i m, a)
- Some important labels (`clefG`, `clefF`, `noteheadHalf`) are well detected on some images
and badly detected on others, and yet both images exhibit good (synthetic) quality.


The last point is strange. 
Perhaps this is due to the image interline value.
I think I remember that most, if not all, scores of the DS2 dataset exhibit an interline value of 23 pixels.
To be further investigated. 