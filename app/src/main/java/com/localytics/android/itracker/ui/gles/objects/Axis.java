package com.localytics.android.itracker.ui.gles.objects;

import com.localytics.android.itracker.ui.gles.data.VertexArray;
import com.localytics.android.itracker.ui.gles.objects.ObjectBuilder.DrawCommand;
import com.localytics.android.itracker.ui.gles.objects.ObjectBuilder.GeneratedData;
import com.localytics.android.itracker.ui.gles.programs.ColorShaderProgram;
import com.localytics.android.itracker.ui.gles.util.Geometry.Cylinder;
import com.localytics.android.itracker.ui.gles.util.Geometry.Point;

import java.util.List;

/**
 * Created by bigbug on 12/21/15.
 */
public class Axis {
    private static final int POSITION_COMPONENT_COUNT = 3;

    public final float mRadius;
    public final float mHeight;

    private final VertexArray mVertexArray;
    private final List<ObjectBuilder.DrawCommand> mDrawList;

    public Axis(float radius, float height, int numOfPoints) {
        GeneratedData generatedData = ObjectBuilder.createAxis(
                new Cylinder(new Point(0f, 0f, 0f), radius, height),
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
