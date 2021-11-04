package com.kdt.localinfo.post.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.kdt.localinfo.category.Category;
import com.kdt.localinfo.comment.entity.Comment;
import com.kdt.localinfo.common.BaseEntity;
import com.kdt.localinfo.photo.Photo;
import com.kdt.localinfo.user.entity.Region;
import com.kdt.localinfo.user.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Table(name = "posts")
@NoArgsConstructor
@Entity
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @Lob
    @Column(name = "contents", nullable = false)
    private String contents;

    @Embedded
    private Region region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_to_category"))
    private Category category;

    @OneToMany(mappedBy = "post", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    @JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_post_to_user"))
    private User user;

    @OneToMany(mappedBy = "post")
    private List<Photo> photos = new ArrayList<>();

    @Builder
    public Post(Long id, String contents, Region region, Category category, List<Photo> photos) {
        this.id = id;
        this.contents = contents;
        this.region = region;
        this.photos = photos;
        setCategory(category);
    }

    //연관관계 편의 메서드 - user
    public void setUser(User user) {
        if (Objects.nonNull(this.user)) {
            this.user.getPosts().remove(this);
        }
        this.user = user;
        user.getPosts().add(this);
    }

    //연관관계 편의 메서드 - comment
    public void addComment(Comment comment) {
        comment.setPost(this);
    }

    public void setComments(List<Comment> comments) {
        comments.forEach(this::addComment);
    }

    //연관관계 편의 메서드 - photo
    public void addPhoto(List<Photo> photos) {
        this.photos = photos;
    }

    //연관관계 편의 메서드 - category
    public void setCategory(Category category) {
        this.category = category;
    }

    public Long updatePost(String contents) {
        this.contents = contents;
        return id;
    }

    public Long deletePost() {
        deletedAt = LocalDateTime.now();
        return id;
    }

}
