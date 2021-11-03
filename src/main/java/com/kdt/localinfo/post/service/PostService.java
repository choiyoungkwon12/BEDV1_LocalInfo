package com.kdt.localinfo.post.service;

import com.kdt.localinfo.photo.Photo;
import com.kdt.localinfo.post.dto.PostCreateRequest;
import com.kdt.localinfo.post.dto.PostResponse;
import com.kdt.localinfo.post.entity.Post;
import com.kdt.localinfo.post.repository.PostRepository;
import javassist.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;

    private final S3Service s3Service;

    public PostService(PostRepository postRepository, S3Service s3Service) {
        this.postRepository = postRepository;
        this.s3Service = s3Service;
    }

    @Transactional
    public Long createPost(PostCreateRequest request) throws IOException {
        List<Photo> photoUrls = new ArrayList<>();
        List<MultipartFile> photos = request.getPhotos();

        if (!Objects.isNull(photos)) {
            for (MultipartFile photo : photos) {
                Photo photoEntity = Photo.builder()
                        .url(s3Service.upload(photo))
                        .build();
                photoUrls.add(photoEntity);
            }
        }

        Post post = request.toEntity(photoUrls);
        Post savedPost = postRepository.save(post);
        return savedPost.getId();
    }

    @Transactional
    public PostResponse findDetailPost(Long postId) throws NotFoundException {
        return PostResponse.of(postRepository.findById(postId)
                .filter(foundPost -> foundPost.getDeletedAt() == null)
                .orElseThrow(() -> new NotFoundException("해당 게시글을 찾을 수 없습니다.")));
    }

    @Transactional
    public List<PostResponse> findAllByCategory(Long categoryId) {
        return postRepository.findPostByCategoryId(categoryId)
                .stream().filter(foundPost -> foundPost.getDeletedAt() == null)
                .map(PostResponse::of)
                .collect(Collectors.toList());
    }
}
