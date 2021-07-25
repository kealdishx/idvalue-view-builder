package view.builder;

import com.github.kealdish.idvalue.view.builder.ViewBuilder;
import com.github.kealdish.idvalue.view.builder.context.impl.SimpleBuildContext;
import com.github.kealdish.idvalue.view.builder.impl.SimpleViewBuilder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import view.builder.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.slf4j.LoggerFactory.getLogger;

public class ViewBuilderTest {
    private static Logger logger = getLogger(ViewBuilderTest.class);
    private TestDAO testDAO;
    private ViewBuilder<SimpleBuildContext> builder;

    @BeforeEach
    void setup() {
        testDAO = new TestDAO();
        builder = new SimpleViewBuilder<SimpleBuildContext>()
                .valueFromSelf(User.class, User::getId)
                .valueFromSelf(Post.class, Post::getId)
                .valueFromSelf(Comment.class, Comment::getId)
                .extractId(Comment.class, Comment::getAtUserIds, User.class)
                .extractId(UserIdInterface.class, UserIdInterface::getUserId, User.class)
                .extractValue(Post.class, Post::comments, Comment::getId, Comment.class)
                .buildValue(User.class, testDAO::getUsers)
                .buildValue(Post.class, testDAO::getPosts)
                .buildValue(Comment.class, testDAO::getComments)
                .buildValueTo(User.class, (SimpleBuildContext context, Collection<Integer> ids) -> testDAO
                                .isFollowing(1, ids),
                        "isFollowing")
                .lazyBuild(User.class, (SimpleBuildContext context, Collection<Integer> ids) -> testDAO
                        .isFans(1, ids), "isFans")
                .lazyBuild(User.class, (SimpleBuildContext context, Collection<Integer> ids) -> {
                    Map<Integer, Boolean> fans = testDAO.isFans(1, ids);
                    logger.debug("build fans for:{}->{}, result:{}", 1, ids,
                            fans);
                    return fans;
                }, "isFans3");
        System.out.println("builder===>");
        System.out.println(builder);
    }

    @Test
    void testBuild() {
        Integer visitorId = 1;
        SimpleBuildContext buildContext = new SimpleBuildContext();
        List<Object> sources = new ArrayList<>();
        Collection<Post> posts = testDAO.getPosts(Arrays.asList(1L, 2L, 3L)).values();
        posts.forEach(post -> post.setComments(
                new ArrayList<>(testDAO.getComments(post.getCommentIds()).values())));
        sources.addAll(posts);
        sources.addAll(testDAO.getComments(singletonList(3L)).values());
        sources.add(new SubUser(98));
        logger.info("sources===>");
        sources.forEach(o -> logger.info("{}", o));
        testDAO.assertOn();
        builder.buildMulti(sources, buildContext);
        logger.info("buildContext===>");
        logger.info("{}", buildContext);

        assertTrue(testDAO.retrievedFansUserIds.isEmpty());

        Map<Integer, Boolean> isFans = buildContext.getData("isFans");
        logger.info("isFans:{}", isFans);
        isFans.forEach((userId, value) -> assertEquals(
                testDAO.fansMap.get(visitorId).contains(userId), value));
        assertFalse(testDAO.retrievedFansUserIds.isEmpty());
        logger.info("retry fans");
        buildContext.getData("isFans");
        logger.info("doing merge");
        buildContext.merge(new SimpleBuildContext());
        testDAO.retrievedFansUserIds.clear();
        logger.info("isFans:{}", buildContext.getData("isFans"));

        // try assert
        for (Object obj : sources) {
            if (obj instanceof Post) {
                Post post = (Post) obj;
                assertEquals(buildContext.getData(Post.class).get(post.getId()), obj);
                assertEquals(post.getUserId(),
                        buildContext.getData(User.class).get(post.getUserId()).getId());
                for (Comment cmt : post.comments()) {
                    assertCmt(buildContext, cmt);
                }
            }
            if (obj instanceof Comment) {
                Comment cmt = (Comment) obj;
                assertCmt(buildContext, cmt);
            }
            if (obj instanceof User) {
                User user = (User) obj;
                assertUser(buildContext, user);
            }
        }

        Map<Long, Boolean> unreachedLazy = buildContext.getData("unreachedLazy2");
        assertTrue(unreachedLazy.isEmpty());
        assertFalse(unreachedLazy.getOrDefault(1L, false));

        logger.info("checking nodes.");
        buildContext.getData(User.class).values().forEach(user -> assertUser(buildContext, user));
        logger.info("fin.");
    }

    @Test
    void testNullBuild() {
        SimpleBuildContext buildContext = new SimpleBuildContext();
        builder.buildSingle(null, buildContext);
        buildContext.getData("t").put("a", "c");
        System.out.println("checking...");
        Map<Integer, Boolean> isFans = buildContext.getData("isFans3");
        assertFalse(isFans.getOrDefault(1, false));
        System.out.println("fin.");
    }

//    @Test
//    void testMerge() {
//        TestBuildContext buildContext = new TestBuildContext(1);
//        List<User> users = new ArrayList<>(testDAO.getUsers(ImmutableList.of(1, 2, 3)).values());
//        builder.buildMulti(users, buildContext);
//        Map<Integer, Boolean> isFans = buildContext.getData("isFans3");
//        System.out.println("isFans:" + isFans);
//        users.forEach(user -> assertNotNull(isFans.get(user.getId())));
//
//        TestBuildContext other = new TestBuildContext(1);
//        List<User> users2 = new ArrayList<>(testDAO.getUsers(ImmutableList.of(3, 4, 5)).values());
//        builder.buildMulti(users2, other);
//        Map<Integer, Boolean> isFans2 = other.getData("isFans3");
//        System.out.println("isFans2:" + isFans2);
//        users2.forEach(user -> assertNotNull(isFans2.get(user.getId())));
//
//        buildContext.merge(other);
//        System.out.println("after merged.");
//        System.out.println("users:" + buildContext.getData(User.class));
//
//        Map<Integer, Boolean> merged = buildContext.getData("isFans3");
//        System.out.println("merged:" + merged);
//        for (int i = 1; i <= 5; i++) {
//            assertNotNull(merged.get(i));
//        }
//        System.out.println("fin.");
//    }

//    @Test
//    void testDuplicateMerge() {
//        TestBuildContext mainBuildContext = new TestBuildContext(1);
//
//        TestBuildContext buildContext = new TestBuildContext(1);
//        builder.buildMulti(emptyMap().values(), buildContext);
//        mainBuildContext.merge(buildContext);
//
//        TestBuildContext buildContext2 = new TestBuildContext(1);
//        Map<Integer, User> byIdsFailFast = testDAO.getUsers(ImmutableList.of(1, 2));
//        builder.buildMulti(byIdsFailFast.values(), buildContext2);
//        Map<Integer, Boolean> isFans3 = buildContext2.getData("isFans3");
//        System.out.println("[test] " + isFans3);
//        assertFalse(isFans3.isEmpty());
//
//        mainBuildContext.merge(buildContext2);
//
//        isFans3 = mainBuildContext.getData("isFans3");
//        System.out.println("[test] " + isFans3);
//        assertFalse(isFans3.isEmpty());
//    }

    private void assertUser(SimpleBuildContext buildContext, User user) {
        assertNotNull(buildContext.getData("isFollowing").get(user.getId()));
    }

    private void assertCmt(SimpleBuildContext buildContext, Comment cmt) {
        assertEquals(buildContext.getData(Comment.class).get(cmt.getId()), cmt);
        assertEquals(cmt.getUserId(),
                buildContext.getData(User.class).get(cmt.getUserId()).getId());
        if (cmt.getAtUserIds() != null) {
            for (Integer atUserId : cmt.getAtUserIds()) {
                assertEquals(atUserId, buildContext.getData(User.class).get(atUserId).getId());
            }
        }
    }

    private class TestDAO {

        private static final int USER_MAX = 100;
        private final Map<Long, Post> posts = ImmutableList
                .of(new Post(1, 1, null),
                        new Post(2, 1, Arrays.asList(1L, 2L, 3L)),
                        new Post(3, 2, Arrays.asList(4L, 5L)))
                .stream().collect(toMap(Post::getId, identity()));

        private final Map<Long, Comment> cmts = ImmutableList
                .of(new Comment(1, 1, null), new Comment(2, 2, null), new Comment(3, 1, null),
                        new Comment(4, 2, Arrays.asList(2, 3)),
                        new Comment(5, 11, Arrays.asList(2, 99)))
                .stream().collect(toMap(Comment::getId, identity()));

        private final Multimap<Integer, Integer> followingMap = HashMultimap.create();
        private final Multimap<Integer, Integer> fansMap = HashMultimap.create();
        private Set<Integer> retreievedUserIds;
        private Set<Long> retreievedPostIds;
        private Set<Long> retreievedCommentIds;
        private Set<Integer> retrievedFollowUserIds;
        private Set<Integer> retrievedFansUserIds;

        {
            followingMap.put(1, 5);
            followingMap.put(1, 2);
        }

        {
            fansMap.put(1, 5);
            fansMap.put(1, 99);
        }

        Map<Integer, User> getUsers(Collection<Integer> ids) {
            if (retreievedUserIds != null) {
                logger.info("try to get users:{}", ids);
                for (Integer id : ids) {
                    assertTrue(retreievedUserIds.add(id));
                }
            }
            return ids.stream().filter(i -> i <= USER_MAX).collect(toMap(identity(), User::new));
        }

        Map<Long, Post> getPosts(Collection<Long> ids) {
            if (retreievedPostIds != null) {
                logger.info("try to get posts:{}", ids);
                for (Long id : ids) {
                    assertTrue(retreievedPostIds.add(id));
                }
            }
            return Maps.filterKeys(posts, ids::contains);
        }

        Map<Long, Comment> getComments(Collection<Long> ids) {
            if (ids == null) {
                return emptyMap();
            }
            if (retreievedCommentIds != null) {
                logger.info("try to get cmts:{}", ids);
                for (Long id : ids) {
                    assertTrue(retreievedCommentIds.add(id));
                }
            }
            return Maps.filterKeys(cmts, ids::contains);
        }

        Map<Integer, Boolean> isFollowing(int fromUserId, Collection<Integer> ids) {
            if (retrievedFollowUserIds != null) {
                logger.info("try to get followings:{}->{}", fromUserId, ids);
                for (Integer id : ids) {
                    assertTrue(retrievedFollowUserIds.add(id));
                }
            }
            Collection<Integer> followings = followingMap.get(fromUserId);
            return ids.stream().collect(toMap(identity(), followings::contains));
        }

        Map<Integer, Boolean> isFans(int fromUserId, Collection<Integer> ids) {
            if (retrievedFansUserIds != null) {
                logger.info("try to get fans:{}->{}", fromUserId, ids);
                for (Integer id : ids) {
                    assertTrue(retrievedFansUserIds.add(id));
                }
            }
            Collection<Integer> fans = fansMap.get(fromUserId);
            return ids.stream().collect(toMap(identity(), fans::contains));
        }

        void assertOn() {
            logger.info("assert on.");
            retreievedUserIds = new HashSet<>();
            retreievedPostIds = new HashSet<>();
            retreievedCommentIds = new HashSet<>();
            retrievedFollowUserIds = new HashSet<>();
            retrievedFansUserIds = new HashSet<>();
        }
    }
}
