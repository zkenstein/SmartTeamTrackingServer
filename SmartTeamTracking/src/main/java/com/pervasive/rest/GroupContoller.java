package com.pervasive.rest;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pervasive.model.Group;
import com.pervasive.model.User;
import com.pervasive.repository.GroupRepository;
import com.pervasive.repository.UserRepository;


@RestController
public class GroupContoller {
	
	@Autowired
	private ApplicationContext context;
	
	static Logger log = Logger.getLogger(GroupContoller.class.getSimpleName());

    
	//Return null if group doesn't exist. Else return list of user of group identified by id. 
    @RequestMapping("/group/{groupId}")
    public Set<User> getUsers(@PathVariable Long groupId) {
    	Group groupFromNeo;
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);

    	Transaction tx = graphDatabaseService.beginTx();
		try{
			
			groupFromNeo = groupRepository.findById(groupId);
			if( groupFromNeo == null){
				tx.success();
				tx.close();
		    	log.info("Called /group/"+groupId+" resource. Can't find group, returning null");
				return null;
			}
		    tx.success();				
        	}
		
		finally{
			tx.close();
		}
    	log.info("Called /group/"+groupId+" resource. Returning "+groupFromNeo.getContains().toString());
		return groupFromNeo.getContains();    	
    }
    
    //Returns -1 if can't find group, else returns number of users of a Group. 
    @RequestMapping("/group/{groupId}/count")
    public int getCountUsers(@PathVariable Long groupId) {
    	
    	Group groupFromNeo;
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);

    	Transaction tx = graphDatabaseService.beginTx();
		try{
			groupFromNeo = groupRepository.findById(groupId);
			if( groupFromNeo == null){
				tx.success();
				tx.close();
		    	log.info("Called /group/"+groupId+"/count resource. Can't find group, returning -1");
				return -1;
			}
        }
		finally{
			tx.close();
		}
    	log.info("Called /group/"+groupId+"/count resource. Returning "+ groupFromNeo.getContains().size());
		return groupFromNeo.getContains().size();
    }
    
 
    //Creates a new group, adds group creator and returns group identifier. In case of error returns -1. 
    @RequestMapping(method = RequestMethod.POST,value="/group")
    public long createGroup(@RequestParam(value ="userId", defaultValue="null") Long userId,
    						@RequestParam(value="name", defaultValue="null") String name,
    						@RequestParam(value="lat", defaultValue="null") Double latitude,
    						@RequestParam(value="lon", defaultValue="null") Double longitude,
    						@RequestParam(value="radius", defaultValue="30") int radius){
       
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
       
        long result = 0; 
        
        //Should also directly add itself to GROUP
    	Transaction tx = graphDatabaseService.beginTx();
		try{
			User userFromNeo = userRepository.findById(userId);
			if(userFromNeo == null){
				tx.success();
				tx.close();
				log.info("Called POST /group resource. Can't find user, returning -1");
				return -1;			
			}
	
			Group newGroup = new Group(name, latitude, longitude, radius);
			newGroup.addUser(userFromNeo);
			newGroup = groupRepository.save(newGroup);
			
			if(newGroup.getId() == null){
				tx.success();
				tx.close();
				log.info("Called POST /group resource. Error in saving group, returning -1");
				return -1;
			}
			
			result = newGroup.getId();
			tx.success();
        }
		finally{
			tx.close();
		}
		log.info("Called POST /group resource. Returning group identifier: "+result);
		return result;
    }
    
    // Invite users in a group identified by groupId. List of users is sent as a list of facebookId in the body. 
    // Returns null if can't find group, else returns the list of users facebookId's successfully invited. 
    @RequestMapping(method = RequestMethod.POST,value="/group/{groupId}/invite", consumes = "application/json")
    public List<String> inviteUserToGroup(@PathVariable Long groupId, @RequestBody List<String> fbUsers){
    	
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
  
    	List<String> invitedUsers = new LinkedList<String>();  	
        Transaction tx = graphDatabaseService.beginTx();
		try{
			Group groupFromNeo = groupRepository.findById(groupId);
			if(groupFromNeo == null){
				log.info("Called POST /group/"+groupId+"/invite resource. Cant' find group, returning null");
				tx.success();
				tx.close();
				return null;
			}
			for(String facebookId: fbUsers){
				User userToAdd = userRepository.findByFacebookId(facebookId);
				if(userToAdd != null) {
					invitedUsers.add(userToAdd.getFacebookId());
					groupFromNeo.addUserPending(userToAdd);
				}
			}
			groupRepository.save(groupFromNeo);
			
			tx.success();
        	}
		
		finally{
			tx.close();
		}
		log.info("Called POST /group/"+groupId+"/invite resource. Returning "+invitedUsers.toString());
		return invitedUsers;
    }
    
    
	
	
	

}
