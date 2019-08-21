/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.apache.mxnet.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import software.amazon.ai.ndarray.NDArray;
import software.amazon.ai.ndarray.types.DataType;
import software.amazon.ai.ndarray.types.Shape;
import software.amazon.ai.repository.Artifact;
import software.amazon.ai.util.Pair;
import software.amazon.ai.util.Utils;

/**
 * MNIST handwritten digits dataset from http://yann.lecun.com/exdb/mnist
 *
 * <p>Each sample is an image (in 3D NDArray) with shape (28, 28, 1).
 */
public final class Mnist extends SimpleDataset {

    private static final String ARTIFACT_ID = "mnist";

    public Mnist(Builder builder) {
        super(builder);
    }

    @Override
    public String getArtifactID() {
        return ARTIFACT_ID;
    }

    @Override
    public Pair<NDArray, NDArray> get(long index) {
        return new Pair<>(data.get(index), labels.get(index));
    }

    @Override
    public void loadData(Usage usage) throws IOException {
        Map<String, Artifact.Item> map = artifact.getFiles();
        Artifact.Item imageItem;
        Artifact.Item labelItem;
        switch (usage) {
            case TRAIN:
                imageItem = map.get("train_data");
                labelItem = map.get("train_labels");
                break;
            case TEST:
                imageItem = map.get("test_data");
                labelItem = map.get("test_labels");
                break;
            case VALIDATION:
            default:
                throw new UnsupportedOperationException("Validation data not available.");
        }
        labels = readLabel(labelItem);
        size = labels.size();
        data = readData(imageItem, labels.size());
    }

    private NDArray readData(Artifact.Item item, long length) throws IOException {
        try (InputStream is = repository.openStream(item, null)) {
            if (is.skip(16) != 16) {
                throw new AssertionError("Failed skip data.");
            }

            byte[] buf = Utils.toByteArray(is);
            try (NDArray array = manager.create(new Shape(length, 28, 28, 1), DataType.UINT8)) {
                array.set(buf);
                return array.asType(DataType.FLOAT32, true);
            }
        }
    }

    private NDArray readLabel(Artifact.Item item) throws IOException {
        try (InputStream is = repository.openStream(item, null)) {
            if (is.skip(8) != 8) {
                throw new AssertionError("Failed skip data.");
            }

            byte[] buf = Utils.toByteArray(is);
            try (NDArray array = manager.create(new Shape(buf.length), DataType.UINT8)) {
                array.set(buf);
                return array.asType(DataType.FLOAT32, true);
            }
        }
    }

    public static class Builder extends SimpleDataset.BaseBuilder<Builder> {

        @Override
        public Builder self() {
            return this;
        }

        public Mnist build() {
            return new Mnist(this);
        }
    }
}
