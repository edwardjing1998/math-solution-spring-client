package com.example.mathsolution.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MathSolutionResponse(
        String status,
        @JsonProperty("input_blob_path") String inputBlobPath,
        @JsonProperty("output_blob_path") String outputBlobPath,
        @JsonProperty("content_type") String contentType
) {
}

