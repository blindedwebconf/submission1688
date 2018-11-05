package knowledgegraphpartitioning;
import java.util.LinkedList;


/**
 * Tree Class
 * Defines a tree node to build a tree structure
 * Can store any kind of object
 * Has a parent and a list of children
 * Has a level (root is level 0, every childs level is one bigger than its parents level)
 * @author ---
 *
 * @param <T>
 */
public class TreeNode<T> 
{
    private LinkedList<TreeNode<T>> children = new LinkedList<TreeNode<T>>(); //List of children
    private TreeNode<T> parent = null; //Parent node of this node (null if root)
    private T data = null; //Data contained within this node
    private int level; //Depth of this node, 0 for root, parent has level x then child has level x + 1;

    /**
     * creates a Root node
     * @param data to be stored in node.
     */
    public TreeNode(T data) 
    {
        this.data = data;
        this.level = 0;
    }

    /**
     * 
     * @return Linked
     */
    public LinkedList<TreeNode<T>> getChildren() 
    {
        return children;
    }


    /**
     * Creates a new TreeNode.
     * Adds this Node to children.
     * @param data to be stored in the node
     */
    public void addChild(T data) 
    {
        TreeNode<T> child = new TreeNode<T>(data); 	//create a new node (child)
        child.parent = this; 						//Set this node to child's parent
        child.level = this.level + 1;				//Set child's level
        this.children.add(child);					//Add child to list of this nodes children
    }

    /**
     * Adds a given Node to children. Changes its parent and level.
     * @param child Node to be added as child
     */
    public void addChild(TreeNode<T> child) 
    {
    	child.parent = this;						//Set this node to parent of the "child"
    	child.level = this.level + 1;				//Set level of child
        this.children.add(child);					//Add child to list of this nodes children
    }

    /**
     * 
     * @return data stored in node
     */
    public T getData() 
    {
        return this.data;
    }

    /**
     * Sets data stored in node
     * @param data
     */
    public void setData(T data) 
    {
        this.data = data;
    }

    /**
     * 
     * @return true if node is root, false otherwise.
     */
    public boolean isRoot() 
    {
        return (this.parent == null);
    }

    /**
     * 
     * @return true if node is leave false if it is not.
     */
    public boolean isLeaf() 
    {
        if(this.children.size() == 0) 
            return true;
        else 
            return false;
    }
    
    /**
     * 
     * @return level of node
     */
    public int getLevel()
    {
    	return level;
    }

    
    /**
     * Finds all nodes on a certain level. Root has level 0.
     * Every childs level is 1 bigger then its parents level.
     * If this is not called on the root only nodes in the subtree with the given level (still distance to root) are found.
     * @param level
     * @return LinkedList<TreeNode<T>> of nodes on given level.
     */
    public LinkedList<TreeNode<T>> getNodesOnLevel(int level)
    {
    	// List to store found nodes
    	LinkedList<TreeNode<T>> nodesOnLevel = new LinkedList<TreeNode<T>>();
    	// End of recursion, return all children of level -1 = all nodes on level
    	if(this.level == level - 1)
    	{
    		return children;
    	} // If level has not been reached yet recusively call function for all children and return found nodes
    	else if (this.level < level - 1)
    	{
    		for(TreeNode<T> child : children)
    		{
    			nodesOnLevel.addAll(child.getNodesOnLevel(level));
    		}
    		return nodesOnLevel;
    	} // Special case where function is called for level 0 = root
    	else if (level == 0 && this.level == 0)
    	{
    		nodesOnLevel.add(this);
    		return nodesOnLevel;
    	} // If this function is called on a starting node with level higher then level returns null
    	else if(this.level >= level)
    	{
    		return null;
    	}
    	return null;
    }
    
    /**
     * Finds all leaves. If this is called on an other node then the root it finds all leaves of the subtree where this node would be the root
     * @return a LinkedListr<TreeNode<T>> of all leaves of the subtree with this node as root.
     */
    public LinkedList<TreeNode<T>> getLeaves()
    {
    	// List of found leaves
    	LinkedList<TreeNode<T>> leaves = new LinkedList<TreeNode<T>>();
    	// If node is leave add to list and return
    	if(this.isLeaf())
    	{
    		leaves.add(this);
    		return leaves;
    	} else // Else recursively check if children are leaves
    	{
    		for(TreeNode<T> child : children)
    		{
    			leaves.addAll(child.getLeaves());
    		}
    	}
    	return leaves;
    }
    
    /**
     * Removes all children of this node.
     */
    public void removeChildren()
    {
    	children.clear();
    }
}
