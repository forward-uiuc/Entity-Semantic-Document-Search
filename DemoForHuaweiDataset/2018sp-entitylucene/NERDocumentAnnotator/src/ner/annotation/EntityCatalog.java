package ner.annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Stack;

import ner.annotation.treegazetteer.GazetteerTree;

/**
 * A class for defining and referencing EntityTypes
 * This class also holds EntityAnnotations generated by NERTechniques
 * This class also holds a Gazette
 * @author aaulabaugh@gmail.com
 */

public class EntityCatalog
{
	
	//An array of named entity information which is built by at least one NER technique
	protected ArrayList<EntityAnnotation> annotationArray;
		
	//All entity types supported
	protected Hashtable<String, EntityType> ENTITY_TYPES;
	
	//Holds the gazetteer entries in a table
	protected GazetteerTable gzTable;
	
	//Holds the gazetteer entries in a tree
	protected GazetteerTree gzTree;
	
	//Holds the gazetteeer entries in a list
	protected ArrayList<EntityInstance> gzList;
	
	public EntityCatalog()
	{
		annotationArray = new ArrayList<EntityAnnotation>();
		ENTITY_TYPES = new Hashtable<String, EntityType>();
		gzTable = null;
		gzTree = null;
		gzList = null;;
	}
	
	//Resetters
	
	public void setGazetteerTable(GazetteerTable inTable)
	{
		gzTable = inTable;
	}
	
	public void setGazetteerTree(GazetteerTree inTree)
	{
		gzTree = inTree;
	}
	
	public void setGazetteerList(ArrayList<EntityInstance> inList)
	{
		gzList = inList;
	}
	
	/**
	 * Clears the EntityAnnotations
	 */
	public void resetAnnotations()
	{
		annotationArray = new ArrayList<EntityAnnotation>();
	}
	
	
	/**
	 * Clears all EntityTypes
	 */
	public void resetEntityTypes()
	{
		ENTITY_TYPES = new Hashtable<String, EntityType>();
	}
	
	//Setters
	
	/**
	 * Sets this EntityAnnotation's and it's childrens' termNums to the input termNum
	 * This is a recursive helper method for finalizeAnnotation()
	 * @param annotation the EntityAnnotation to modify (and it's children)
	 * @param termNum what their term num becomes
	 */
	private void setAllTermNum(EntityAnnotation annotation, int termNum)
	{
		annotation.setTermNum(termNum);
		for(EntityAnnotation subAnnotation : annotation.getChildren())
		{
			setAllTermNum(subAnnotation, termNum);
		}
	}
	
	/**
	 * Called by NER techniques, this adds more information to the EntityAnnotation array.
	 * Adds EntityAnnotation such that the longest tokens are at the top level.
	 * @param newAnnotation a token and an entity classification provided by a NER technique
	 */
	public void addAnnotation(EntityAnnotation newAnnotation)
	{
		boolean added = false;
		EntityAnnotation toRemove = null;
		for(EntityAnnotation existingAnnotation : annotationArray)
		{
			if(existingAnnotation.isSuperToken(newAnnotation) && !added)
			{
				existingAnnotation.addAnnotation(newAnnotation);
				return;
			}
			else if(newAnnotation.isSuperToken(existingAnnotation) && !added)
			{
				newAnnotation.addAnnotation(existingAnnotation);
				toRemove = existingAnnotation;
				break;
			}
		}
		if(toRemove != null)
		{
			annotationArray.remove(toRemove);
		}
		annotationArray.add(newAnnotation);
	}
	
	/**
	 * Adds an Entity of type t to the catalog
	 * @param t
	 */
	public void addEntityType(EntityType t)
	{
		ENTITY_TYPES.put(t.getID(), t);
	}
	
	public void addInstanceToList(EntityInstance inst)
	{
		if(gzList!= null)
			gzList.add(inst);
	}
	
	//getters
	
	public GazetteerTable getGazetteerTable()
	{
		return gzTable;
	}
	
	public GazetteerTree getGazetteerTree()
	{
		return gzTree;
	}
	
	public ArrayList<EntityInstance> getGazetteerList()
	{
		return gzList;
	}
	
	/**
	 * Returns all EntityAnnotations
	 * @return
	 */
	public ArrayList<EntityAnnotation> getAnnotaitons()
	{
		return annotationArray;
	}
	
	
	public boolean containsEntityType(String idIn)
	{
		return ENTITY_TYPES.containsKey(idIn.toLowerCase());
	}
	
	/**
	 * Fetches an EntityType with an id of idIn
	 * if it exists in the hash. Otherwise it returns null.
	 * @param idIn
	 * @return
	 */
	public EntityType getEntityType(String idIn)
	{
		String idInLower = idIn.toLowerCase();
		if(ENTITY_TYPES.containsKey(idInLower))
		{
			return ENTITY_TYPES.get(idInLower);
		}
		else
		{
			Exception e = new Exception("ENTITY TYPE '" + idIn + "' NOT IN CATALOG");
			e.printStackTrace();
		}
		return null;
	}
	
	//Other
	
	/**
	 * Assigns the final terms to the remaining EntityAnnotation
	 * All subAnnotation termNums are equivalent to their parent's termNum
	 */
	public void finalizeAnnotation()
	{
		int termNum = 0;
		for(EntityAnnotation annotation: annotationArray)
		{
			setAllTermNum(annotation, termNum);
			termNum +=1;
		}
	}
	
	/**
	 * Reconciles the annotationArray, generating the final entities and tokens
	 */
	public void reconcileAnnotation(AnnotationReconciler reconciler)
	{
		Collections.sort(annotationArray);
		for(EntityAnnotation e : annotationArray)
		{
			if(e.getChildren().size() > 0)
			{
				reconciler.reconcileAnnotationTree(e, this);
			}
		}
		finalizeAnnotation();
	}
	
	public EntityCatalog cloneSelf()
	{
		EntityCatalog myClone = new EntityCatalog();
		for(String myType : ENTITY_TYPES.keySet())
		{
			myClone.addEntityType(ENTITY_TYPES.get(myType));
		}
		myClone.setGazetteerTable(gzTable);
		myClone.setGazetteerTree(gzTree);
		myClone.setGazetteerList(gzList);
		return myClone;
	}
	
	public ArrayList<String> getEntityTypes()
	{
		ArrayList<String> allEntityTypes = new ArrayList<String>(); 
		for(String myType : ENTITY_TYPES.keySet())
		{
			allEntityTypes.add(myType);
		}
		return allEntityTypes;
	}
	
	/**
	 * Generates all super types of a list of entity types.
	 * @param baseType
	 * @return
	 */
	public ArrayList<EntityType> getSuperEntityTypes(ArrayList<EntityType> baseTypes)
	{
		Stack<EntityType> stack = new Stack<EntityType>();
		HashSet<EntityType> resultSet = new HashSet<EntityType>();
		stack.addAll(baseTypes);
		while(!stack.isEmpty())
		{
			EntityType toExpand = stack.pop();
			if(!resultSet.contains(toExpand))
				resultSet.add(toExpand);
			stack.addAll(toExpand.getSuperTypes());
		}
		return new ArrayList<EntityType>(resultSet);
	}
}
