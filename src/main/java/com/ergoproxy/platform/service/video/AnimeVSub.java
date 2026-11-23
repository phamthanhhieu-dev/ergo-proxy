package com.ergoproxy.platform.service.video;

import com.ergoproxy.platform.common.constant.Const;
import com.ergoproxy.platform.common.exception.LogicException;
import com.ergoproxy.platform.model.dto.video.SearchRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnimeVSub implements VideoRouter {

    @Override
    public String getSource() {
        return Const.Source.ANIMEVSUB;
    }

    @Override
    public ResponseEntity<?> search(SearchRequestDTO searchRequestDTO) {
        List<Object> response = new ArrayList<>();
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> getDetail(String query) {
        return null;
    }
}
