package org.elmo.robella.service;

import org.elmo.robella.mapper.ModelMapper;
import org.elmo.robella.model.entity.Model;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    public List<Model> findByPublishedTrue() {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Model::getPublished, true);
        return this.list(queryWrapper);
    }

    public List<Model> searchModels(String keyword) {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Model::getName, keyword)
                   .or()
                   .like(Model::getModelKey, keyword)
                   .or()
                   .like(Model::getDescription, keyword);
        return this.list(queryWrapper);
    }

    public List<Model> findByCapability(String capability) {
        LambdaQueryWrapper<Model> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Model::getCapabilities, capability);
        return this.list(queryWrapper);
    }
}
