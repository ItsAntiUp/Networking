package com.routing;

import java.util.Comparator;

class NodeComparator implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
        Integer id1 = ((Node)o1).getNodeId();
        Integer id2 = ((Node)o2).getNodeId();
        return id1.compareTo(id2);
    }
}
