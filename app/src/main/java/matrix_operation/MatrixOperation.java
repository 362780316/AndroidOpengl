package matrix_operation;

import android.text.style.IconMarginSpan;

import com.google.android.filament.utils.Float3;
import com.google.android.filament.utils.Mat3;
import com.google.android.filament.utils.Mat4;
import com.google.android.filament.utils.VectorKt;

import java.util.Vector;

public class MatrixOperation {

    public static Mat3 eulerAnglesToRotationMatrix(float[] eulerAngles){
//        Mat3 mat4 = new Mat4(new Float3(),new Float3(),new Float3(),new Float3());
        return nEulerAnglesToRotationMatrix(eulerAngles);
    }

    public static native Mat3 nEulerAnglesToRotationMatrix(float[] eulerAngles);
}
