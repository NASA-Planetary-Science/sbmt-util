package edu.jhuapl.sbmt.util.users;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class UserCollection
{
    public static UserCollection of(Iterable<User> users)
    {
        Preconditions.checkNotNull(users);

        LinkedHashMap<String, User> usersMap = new LinkedHashMap<>();
        LinkedHashSet<String> groupIds = new LinkedHashSet<>();

        for (User user : users)
        {
            usersMap.put(user.getId(), user);
            groupIds.addAll(user.getGroupIds());
        }

        return new UserCollection(usersMap, groupIds);
    }

    public static UserCollection of(Iterable<User> users, Iterable<String> groupIds)
    {
        Preconditions.checkNotNull(users);
        Preconditions.checkNotNull(groupIds);

        LinkedHashMap<String, User> usersMap = new LinkedHashMap<>();
        for (User user : users)
        {
            usersMap.put(user.getId(), user);
        }

        LinkedHashSet<String> groupIdsList = new LinkedHashSet<>();
        for (String groupId : groupIds)
        {
            groupIdsList.add(groupId);
        }

        return new UserCollection(usersMap, groupIdsList);
    }

    private final LinkedHashMap<String, User> users;
    private final LinkedHashSet<String> groupIds;

    protected UserCollection(LinkedHashMap<String, User> users, LinkedHashSet<String> groupIds)
    {
        this.users = users;
        this.groupIds = groupIds;
    }

    /**
     * Return the user in the collection that matches the specified identifier
     * string, or null if the collection has no user that matches the
     * identifier.
     *
     * @param id the identifier to match
     * @return the matching {@link User} object, or null
     * @throws NullPointerException if id is null
     */
    public User getUser(String id)
    {
        Preconditions.checkNotNull(id);

        return users.get(id);
    }

    public ImmutableList<User> getUsers()
    {
        return ImmutableList.copyOf(users.values());
    }

    public ImmutableList<String> getGroupIds()
    {
        return ImmutableList.copyOf(groupIds);
    }

}
