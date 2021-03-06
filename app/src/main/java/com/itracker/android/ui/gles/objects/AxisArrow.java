package com.itracker.android.ui.gles.objects;

import com.itracker.android.ui.gles.data.VertexArray;
import com.itracker.android.ui.gles.objects.ObjectBuilder.DrawCommand;
import com.itracker.android.ui.gles.objects.ObjectBuilder.GeneratedData;
import com.itracker.android.ui.gles.programs.ColorShaderProgram;
import com.itracker.android.ui.gles.util.Geometry.Cone;
import com.itracker.android.ui.gles.util.Geometry.Point;

import java.util.List;

/**
 * Created by bbo on 12/22/15.
 */
public class AxisArrow {
    private static final int POSITION_COMPONENT_COUNT = 3;

    public final float mRadius;
    public final float mHeight;

    private final VertexArray mVertexArray;
    private final List<DrawCommand> mDrawList;

    public AxisArrow(float radius, float height, int numOfPoints) {
        GeneratedData generatedData = ObjectBuilder.createAxisArrow(
                new Cone(new Point(0f, height, 0f), radius, height),
                numOfPoints);

        mRadius = radius;
        mHeight = height;

        mVertexArray = new VertexArray(generatedData.mVertexData);
        mDrawList = generatedData.mDrawList;
    }

    public void bindData(ColorShaderProgram colorProgram) {
        mVertexArray.setVertexAttribPointer(
                0,
                colorProgram.getPositionAttributeLocation(),
                POSITION_COMPONENT_COUNT, 0);
    }

    public void draw() {
        for (DrawCommand drawCommand : mDrawList) {
            drawCommand.draw();
        }
    }
}
