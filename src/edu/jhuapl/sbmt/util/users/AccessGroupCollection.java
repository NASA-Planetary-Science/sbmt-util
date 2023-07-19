package edu.jhuapl.sbmt.util.users;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class AccessGroupCollection
{
    public static AccessGroupCollection of(Iterable<AccessGroup> groups)
    {
        AccessGroupCollection result = new AccessGroupCollection(new LinkedHashMap<>());

        result.setGroups(groups);

        return result;
    }

    private final LinkedHashMap<String, AccessGroup> collection;

    protected AccessGroupCollection(LinkedHashMap<String, AccessGroup> collection)
    {
        this.collection = collection;
    }

    public AccessGroup getGroup(String id)
    {
        AccessGroup result = collection.get(id);
        Preconditions.checkArgument(result != null, "Id " + id + " not found in collection");

        return result;
    }

    public ImmutableList<AccessGroup> getGroups()
    {
        return ImmutableList.copyOf(collection.values());
    }

    public void setGroups(Iterable<AccessGroup> groups)
    {
        Preconditions.checkNotNull(groups);

        // Make sure no groups share the same ID.
        Set<String> groupIds = new HashSet<>();
        for (AccessGroup group : groups)
        {
            String groupId = group.getId();
            Preconditions.checkArgument(!groupIds.contains(groupId));
            groupIds.add(groupId);
        }

        collection.clear();

        for (AccessGroup group : groups)
        {
            collection.put(group.getId(), group);
        }
    }

    public Map<String, Set<String>> getURLToGroupIdMap()
    {
        Map<String, Set<String>> result = new HashMap<>();

        for (AccessGroup group : collection.values())
        {
            for (String filePath : group.getAuthorizedFilePaths())
            {
                Set<String> groupIds = result.get(filePath);
                if (groupIds == null)
                {
                    groupIds = new HashSet<>();
                    result.put(filePath, groupIds);
                }

                groupIds.add(group.getId());
            }
        }

        return result;
    }
}
