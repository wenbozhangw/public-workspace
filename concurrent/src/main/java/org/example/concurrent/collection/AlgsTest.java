package org.example.concurrent.collection;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlgsTest {

    public static void main(String[] args) {
        buildTree(new int[]{9,3,15,20,7}, new int[]{9,15,7,20,3});

        List<Integer> res = new ArrayList<>();
        for (int j = 1; j <= n; j++) {
            if (tags[j] == 0) {
                res.add(j + 1);
            }
        }
    }

    static Map<Integer, Integer> map = new HashMap<>();
    static int rootIndex = 0;

    public static TreeNode buildTree(int[] inorder, int[] postorder) {
        for (int i = 0; i < inorder.length; i++) {
            map.put(inorder[i], i);
        }
        rootIndex = postorder.length - 1;
        return buildTree(postorder, 0, rootIndex);
    }

    private static TreeNode buildTree(int[] postorder,
                               int left, int right) {
        if (left <= right) {
            int rootVal = postorder[rootIndex];
            TreeNode root = new TreeNode(rootVal);
            rootIndex--;
            root.right = buildTree(postorder, map.get(rootVal) + 1, right);
            root.left = buildTree(postorder, left, map.get(rootVal) - 1);
            return root;
        } else {
            return null;
        }
    }

    public static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode() {
        }

        TreeNode(int val) {
            this.val = val;
        }

        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    public static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }
}
