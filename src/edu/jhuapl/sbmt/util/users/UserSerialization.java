package edu.jhuapl.sbmt.util.users;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import edu.jhuapl.ses.jsqrl.api.Key;
import edu.jhuapl.ses.jsqrl.api.Version;
import edu.jhuapl.ses.jsqrl.impl.InstanceGetter;
import edu.jhuapl.ses.jsqrl.impl.SettableMetadata;

public class UserSerialization
{
    private static volatile boolean initialized = false;
    public static synchronized void initializeSerializationProxies()
    {
        if (!initialized) {
            Version version = Version.of(1, 0);

            {
                Key<AccessGroup> proxyKey = Key.of("AccessGroup");
                Key<String> idKey = Key.of("id");
                Key<List<String>> directoriesKey = Key.of("directories");

                InstanceGetter.defaultInstanceGetter().register(proxyKey, metadata -> {
                    String id = metadata.get(idKey);
                    List<String> directories = metadata.get(directoriesKey);

                    return AccessGroup.of(id, directories);
                }, AccessGroup.class, source -> {
                    SettableMetadata metadata = SettableMetadata.of(version);
                    metadata.put(idKey, source.getId());
                    metadata.put(directoriesKey, source.getAuthorizedFilePaths());

                    return metadata;
                });
            }

            {
                Key<AccessGroupCollection> proxyKey = Key.of("AccessGroupCollection");
                Key<List<AccessGroup>> groupsKey = Key.of("groups");

                InstanceGetter.defaultInstanceGetter().register(proxyKey, metadata -> {
                    List<AccessGroup> groups = metadata.get(groupsKey);

                    return AccessGroupCollection.of(groups);
                }, AccessGroupCollection.class, source -> {
                    SettableMetadata metadata = SettableMetadata.of(version);
                    metadata.put(groupsKey, source.getGroups());

                    return metadata;
                });
            }

            {
                Key<UserCollection> proxyKey = Key.of("UserCollection");
                Key<Map<String, Set<String>>> groupsKey = Key.of("groups");

                InstanceGetter.defaultInstanceGetter().register(proxyKey, metadata -> {
                    Map<String, Set<String>> groups = metadata.get(groupsKey);

                    return UserCollection.of(extractUsers(groups), groups.keySet());
                }, UserCollection.class, source -> {
                    SettableMetadata metadata = SettableMetadata.of(version);

                    metadata.put(groupsKey, extractGroups(source));

                    return metadata;
                });
            }

            initialized = true;
       }

    }

    private static Map<String, Set<String>> extractGroups(UserCollection collection)
    {
        LinkedHashMap<String, Set<String>> groupsToUsersMap = new LinkedHashMap<>();
        for (String groupId : collection.getGroupIds())
        {
            for (User user : collection.getUsers())
            {
                if (user.isInGroup(groupId))
                {
                    Set<String> userIds = groupsToUsersMap.get(groupId);
                    if (userIds == null)
                    {
                        userIds = new LinkedHashSet<>();
                        groupsToUsersMap.put(groupId, userIds);
                    }

                    userIds.add(user.getId());
                }
            }
        }

        return groupsToUsersMap;
    }

    private static ImmutableList<User> extractUsers(Map<String, Set<String>> groupsToUsersMap)
    {
        Map<String, Set<String>> usersToGroupsMap = new LinkedHashMap<>();

        for (Entry<String, Set<String>> entry : groupsToUsersMap.entrySet())
        {
            String groupId = entry.getKey();
            Set<String> userIds = entry.getValue();

            for (String userId : userIds)
            {
                Set<String> groupIds = usersToGroupsMap.get(userId);
                if (groupIds == null)
                {
                    groupIds = new LinkedHashSet<>();
                    usersToGroupsMap.put(userId, groupIds);
                }

                groupIds.add(groupId);
            }

        }

        ImmutableList.Builder<User> builder = ImmutableList.builder();
        for (Entry<String, Set<String>> entry : usersToGroupsMap.entrySet())
        {
            String userId = entry.getKey();
            Set<String> groupIds = entry.getValue();
            builder.add(User.of(userId, groupIds));
        }

        return builder.build();
    }

}
