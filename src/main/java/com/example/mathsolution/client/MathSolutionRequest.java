package com.example.mathsolution.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MathSolutionRequest(
        @NotBlank
        @Pattern(regexp = "^(?!/)(?!.*(?:^|/)\\.\\.?(/|$)).+\\.png$",
                message = "input_blob_path must be a safe relative PNG path")
        @JsonProperty("input_blob_path")
        String inputBlobPath,

        @NotBlank
        @Pattern(regexp = "^(?!/)(?!.*(?:^|/)\\.\\.?(/|$)).+\\.html?$",
                message = "output_blob_path must be a safe relative HTML path")
        @JsonProperty("output_blob_path")
        String outputBlobPath,

        @NotBlank
        @Pattern(regexp = "zh-CN|en-US", message = "language must be zh-CN or en-US")
        String language
) {
}
