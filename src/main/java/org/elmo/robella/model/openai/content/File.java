package org.elmo.robella.model.openai.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class File {

    @JsonProperty("file_data")
    private String fileData;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("file_id")
    private String fileId;

}
