package com.kdt.localinfo.comment.service;

import com.kdt.localinfo.aws.service.AwsS3Service;
import com.kdt.localinfo.comment.converter.CommentConverter;
import com.kdt.localinfo.comment.dto.CommentChangeRequest;
import com.kdt.localinfo.comment.dto.CommentDepth;
import com.kdt.localinfo.comment.dto.CommentResponse;
import com.kdt.localinfo.comment.dto.CommentSaveRequest;
import com.kdt.localinfo.comment.entity.Comment;
import com.kdt.localinfo.comment.repository.CommentRepository;
import com.kdt.localinfo.common.TestEntityFactory;
import com.kdt.localinfo.photo.CommentPhoto;
import com.kdt.localinfo.photo.CommentPhotoRepository;
import com.kdt.localinfo.post.entity.Post;
import com.kdt.localinfo.post.repository.PostRepository;
import com.kdt.localinfo.user.entity.User;
import com.kdt.localinfo.user.repository.UserRepository;
import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@Slf4j
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentConverter commentConverter;
    @Mock
    private AwsS3Service s3Uploader;
    @Mock
    private CommentPhotoRepository commentPhotoRepository;

    @Test
    @Transactional
    @DisplayName("댓글 생성")
    void saveTest() throws NotFoundException, IOException {
        // GIVEN
        String url = "2544a8cf-b522-48f4-915a-6425018c5957-test2.jpg";

        Comment comment = TestEntityFactory.commentBuilder().build();
        User user = comment.getUser();
        Post post = comment.getPost();

        CommentSaveRequest commentSaveRequest = new CommentSaveRequest(user.getId(), "안녕하세요 댓글입니다.");

        CommentResponse expectCommentResponse = new CommentResponse(comment.getId(),
                "안녕하세요 댓글입니다.",
                user.getNickname(),
                LocalDateTime.now(),
                user.getRegion().getNeighborhood(),
                null,
                checkedDepth(comment.getParentId()),
                List.of(url));

        File imageFile = new File(System.getProperty("user.dir") + "/comment-photo/test.jpg");
        MockMultipartFile firstFile = new MockMultipartFile("images", "test.jpg", null, Files.readAllBytes(imageFile.toPath()));

        CommentPhoto commentPhoto = new CommentPhoto(url, comment);
        List<CommentPhoto> commentPhotos = new ArrayList<>();
        commentPhotos.add(commentPhoto);

        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(commentConverter.converterToComment(commentSaveRequest, user, post)).willReturn(comment);
        given(commentConverter.converterToCommentPhoto(comment, url)).willReturn(commentPhoto);
        given(commentRepository.save(comment)).willReturn(comment);
        given(commentPhotoRepository.saveAll(commentPhotos)).willReturn(commentPhotos);
        given(commentConverter.converterToCommentResponse(comment, List.of(url))).willReturn(expectCommentResponse);
        given(s3Uploader.upload(firstFile, "comment-photo")).willReturn(url);

        // WHEN
        CommentResponse commentResponse = commentService.save(commentSaveRequest, post.getId(), List.of(firstFile));

        // THEN
        assertThat(commentResponse.getId(), is(expectCommentResponse.getId()));
        assertThat(commentResponse.getContents(), is(expectCommentResponse.getContents()));
        assertThat(commentResponse.getParentId(), is(expectCommentResponse.getParentId()));
        assertThat(commentResponse.getDepth(), is(expectCommentResponse.getDepth()));
        assertThat(commentResponse.getRegion(), is(expectCommentResponse.getRegion()));
        assertThat(commentResponse.getNickName(), is(expectCommentResponse.getNickName()));
        assertThat(commentResponse.getUrls(), is(expectCommentResponse.getUrls()));
    }

    @Test
    @Transactional(readOnly = true)
    @DisplayName("게시글 아이디로 댓글 조회 Service")
    void findAllByPostIdTest() throws NotFoundException {

        // GIVEN
        Comment comment = TestEntityFactory.commentBuilder().build();

        String url = "2544a8cf-b522-48f4-915a-6425018c5957-test2.jpg";
        List<String> urls = List.of(url);

        CommentResponse commentResponse = new CommentResponse(comment.getId(),
                comment.getContents(),
                comment.getUser().getNickname(),
                comment.getUpdatedAt(),
                comment.getUser().getRegion().getNeighborhood(),
                comment.getParentId(),
                checkedDepth(comment.getParentId())
                , urls);

        List<Comment> returnComments = new ArrayList<>();
        returnComments.add(comment);

        Post post = comment.getPost();
        Long postId = post.getId();

        new CommentPhoto(url, comment);
        given(commentRepository.findAllByPost(post)).willReturn(returnComments);
        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(commentConverter.converterToCommentResponse(comment, urls)).willReturn(commentResponse);

        // WHEN
        List<CommentResponse> commentResponses = commentService.findAllByPostId(postId);

        // THEN
        then(postRepository).should().findById(postId);
        then(commentRepository).should().findAllByPost(post);
        then(commentConverter).should(times(1)).converterToCommentResponse(comment, urls);

        assertThat(commentResponses.size(), is(1));
        commentResponses.forEach(commentResponse1 -> {
            assertThat(commentResponse1.getNickName(), is("nickName"));
            assertThat(commentResponse1.getDepth(), is(0L));
            assertThat(commentResponse1.getContents(), is("댓글"));
            assertThat(commentResponse1.getUrls(), is(urls));
        });
    }

    @Test
    @DisplayName("댓글 수정 Service")
    void changedCommentTest() throws IOException {
        // GIVEN
        Comment comment = TestEntityFactory.commentBuilder().build();
        User user = comment.getUser();

        String changedUrl = "2544a8cf-b522-48f4-915a-6425018c5957-changedTest.jpg";
        File imageFile = new File(System.getProperty("user.dir") + "/comment-photo/test.jpg");
        MockMultipartFile changedFile = new MockMultipartFile("images", "changeTest.jpg", null, Files.readAllBytes(imageFile.toPath()));

        CommentPhoto commentPhoto = new CommentPhoto(changedUrl, comment);

        CommentChangeRequest commentChangeRequest = new CommentChangeRequest(comment.getId(), "수정된 내용", commentPhoto.getCommentPhotoId());

        CommentResponse expectCommentResponse = new CommentResponse(comment.getId(),
                "수정된 내용.",
                user.getNickname(),
                LocalDateTime.now(),
                user.getRegion().getNeighborhood(),
                null,
                checkedDepth(comment.getParentId()),
                List.of(changedUrl));

        given(commentRepository.findById(commentChangeRequest.getCommentId())).willReturn(Optional.of(comment));
        given(commentPhotoRepository.findAllByCommentId(comment.getId())).willReturn(List.of(commentPhoto));
        given(s3Uploader.upload(changedFile, "comment-photo")).willReturn(changedUrl);
        given(commentConverter.converterToCommentResponse(comment, List.of(changedUrl))).willReturn(expectCommentResponse);

        // WHEN
        CommentResponse commentResponse = commentService.changeComment(List.of(changedFile), commentChangeRequest);

        // THEN
        assertThat(commentResponse.getId(), is(expectCommentResponse.getId()));
        assertThat(commentResponse.getContents(), is(expectCommentResponse.getContents()));
        assertThat(commentResponse.getParentId(), is(expectCommentResponse.getParentId()));
        assertThat(commentResponse.getDepth(), is(expectCommentResponse.getDepth()));
        assertThat(commentResponse.getRegion(), is(expectCommentResponse.getRegion()));
        assertThat(commentResponse.getNickName(), is(expectCommentResponse.getNickName()));
        assertThat(commentResponse.getUrls(), is(expectCommentResponse.getUrls()));
    }

    @Test
    @DisplayName("댓글 삭제")
    void deleteComment() throws IOException {
        // GIVEN
        String url = "2544a8cf-b522-48f4-915a-6425018c5957-test2.jpg";

        Comment comment = TestEntityFactory.commentBuilder().build();

        CommentPhoto commentPhoto = new CommentPhoto(url, comment);
        List<CommentPhoto> commentPhotos = new ArrayList<>();
        commentPhotos.add(commentPhoto);

        given(commentRepository.findById(comment.getId())).willReturn(Optional.of(comment));

        // WHEN
        commentService.deleteComment(comment.getId());

        // THEN
        assertThat(comment.getDeletedAt(), is(notNullValue()));
        commentPhotos.forEach(commentPhoto1 -> {
            assertThat(commentPhoto1.getDeletedAt(), is(notNullValue()));
        });


    }

    private Long checkedDepth(Long parentId) {
        return parentId == null ? CommentDepth.ZERO.getDepth() : CommentDepth.ONE.getDepth();
    }
}
