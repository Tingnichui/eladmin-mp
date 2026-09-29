package me.zhengjie.domain.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Getter
@Setter
public class GeneratorSyncRequest {

    private String dataSource = "master";

    @NotEmpty
    private List<String> tables;
}
