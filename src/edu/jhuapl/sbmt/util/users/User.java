package edu.jhuapl.sbmt.util.users;

import java.util.LinkedHashSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class User
{
    private static final User UnauthorizedUser = new User("", new LinkedHashSet<>());

    private static final User PublicUser = of("public", ImmutableList.of());

    /**
     * Create a user with access to the specified groups. All users created with
     * this method are automatically also included in the access group specified
     * by {@link AccessGroup#PublicGroupId}.
     *
     * @param id the user identifier string
     * @param groupIds the access group identifiers of which the user is a
     *            member
     * @return the {@link User} object
     */
    public static User of(String id, Iterable<String> groupIds)
    {
        Preconditions.checkNotNull(groupIds);

        boolean publicGroupIncluded = false;

        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (String groupId : groupIds)
        {
            if (AccessGroup.PublicGroupId.equals(groupId))
            {
                publicGroupIncluded = true;
            }
            builder.add(groupId);
        }

        if (!publicGroupIncluded)
        {
            builder.add(AccessGroup.PublicGroupId);
        }

        return new User(id, new LinkedHashSet<>(builder.build()));
    }

    /**
     * Return a user that is not authorized for access to any
     * {@link AccessGroup}, including any groups with the id
     * {@link AccessGroup#PublicGroupId}.
     */
    public static User ofUnauthorized()
    {
        return UnauthorizedUser;
    }

    /**
     * Return a user that is authorized for access to only {@link AccessGroup}
     * objects with the id {@link AccessGroup#PublicGroupId}.
     */
    public static User ofPublic()
    {
        return PublicUser;
    }

    private final String id;
    private final LinkedHashSet<String> groupIds;

    protected User(String id, LinkedHashSet<String> groupIds)
    {
        this.id = Preconditions.checkNotNull(id);
        this.groupIds = groupIds;
    }

    public String getId()
    {
        return id;
    }

    public boolean isInGroup(String groupId)
    {
        return groupIds.contains(groupId);
    }

    public ImmutableList<String> getGroupIds()
    {
        return ImmutableList.copyOf(groupIds);
    }

    @Override
    public String toString()
    {
        return "User " + getId();
    }

}
