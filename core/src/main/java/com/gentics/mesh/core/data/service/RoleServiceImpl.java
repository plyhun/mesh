package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.awt.print.Pageable;
import java.util.HashSet;
import java.util.Set;

import org.jglue.totorom.FramedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.TPMeshPermission;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.paging.PagingInfo;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;

@Component
public class RoleServiceImpl extends GenericNodeServiceImpl<Role> implements RoleService {

	@Autowired
	protected FramedGraph framedGraph;

	@Autowired
	private PermissionService permissionService;

	@Autowired
	private UserService userService;

	@Autowired
	private MeshSpringConfiguration springConfiguration;

	@Override
	public Role findByUUID(String uuid) {
		return null;
	}

	@Override
	public Role findByName(String name) {
		return null;
	}

	@Override
	public Result<Role> findAll() {
		return null;
	}

	@Override
	public void addPermission(Role role, AbstractPersistable node, PermissionType... permissionTypes) {
		GraphPermission permission = getGraphPermission(role, node);
		// Create a new permission relation when no existing one could be found
		if (permission == null) {
			permission = permissionService.create(role, node);
		}
		for (int i = 0; i < permissionTypes.length; i++) {
			//TODO tinkerpop - handle grant call. Javahandler?
//			permission.grant(permissionTypes[i]);
		}
	}

	@Override
	public GraphPermission getGraphPermission(Role role, AbstractPersistable node) {
		//		return roleRepository.findPermission(role.getId(), node.getId());
		return null;
	}

	@Override
	public GraphPermission revokePermission(Role role, AbstractPersistable node, PermissionType... permissionTypes) {
		GraphPermission permission = getGraphPermission(role, node);
		// Create a new permission relation when no existing one could be found
		if (permission == null) {
			return null;
		}
		for (int i = 0; i < permissionTypes.length; i++) {
			permission.revoke(permissionTypes[i]);
		}
		role.addPermission(permission);
		//		permission = neo4jTemplate.save(permission);
		return permission;
	}

	@Override
	public RoleResponse transformToRest(Role role) {
		if (role == null) {
			throw new HttpStatusCodeErrorException(500, "Role can't be null");
		}
		RoleResponse restRole = new RoleResponse();
		restRole.setUuid(role.getUuid());
		restRole.setName(role.getName());

		for (Group group : role.getGroups()) {
			//			group = neo4jTemplate.fetch(group);
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		return restRole;
	}

	@Override
	public void addCRUDPermissionOnRole(RoutingContext rc, TPMeshPermission meshPermission, GenericNode targetNode) {

		User user = userService.findByUUID(rc.session().getPrincipal().getString("uuid"));

		// 1. Determine all roles that grant given permission
		//		Node userNode = neo4jTemplate.getPersistentState(user);
		Vertex userNode = user.asVertex();
		Set<Role> roles = new HashSet<>();

		//TODO use core blueprint api or gremlin traversal?
		//		for (Edge rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUT)
		//				.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.IN)
		//				.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUT).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		//				.traverse(userNode).relationships()) {
		//
		//			if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getLabel())) {
		//				// Check whether this relation in fact targets our object we want to check
		//				boolean matchesTargetNode = rel.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == meshPermission.getTargetNode().getId();
		//				if (matchesTargetNode) {
		//					// Convert the api relationship to a framed edge
		//					GraphPermission perm = framedGraph.frame(rel, GraphPermission.class);
		//					if (meshPermission.implies(perm) == true) {
		//						// This permission is permitting. Add it to the list of roles
		//						roles.add(perm.getRole());
		//					}
		//				}
		//			}
		//		}

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : roles) {
			addPermission(role, targetNode, PermissionType.CREATE, PermissionType.READ, PermissionType.UPDATE, PermissionType.DELETE);
		}
	}

	@Override
	public Page<Role> findAll(RoutingContext rc, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		//		return findAll(userUuid, new MeshPageRequest(pagingInfo));
		return null;
	}

	@Override
	public Page<Role> findByGroup(RoutingContext rc, Group group, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		//		return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));
		return null;
	}

	public GraphPermission findPermission(Long roleId, Long nodeId) {
		//	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node) WHERE id(node) = {1} AND id(role) = {0} return r")
		return null;
	}

	public Page<Role> findAll(String userUuid, Pageable pageable) {
		//		@Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",

		//		countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		return null;
	}

	public RoleRoot findRoot() {
		//	@Query("MATCH (n:RoleRoot) return n")
		return null;
	}

	//	@Query(value = MATCH_PERMISSION_ON_ROLE + " MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
	//			+ " return role ORDER BY role.name desc",

	//	countQuery = MATCH_PERMISSION_ON_ROLE + "MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
	//			+ "return count(role)")
	Page<Role> findByGroup(String userUuid, Group group, Pageable pageable) {
		return null;
	}

	@Override
	public Role save(Role role) {
		//		RoleRoot root = roleService.findRoot();
		//		if (root == null) {
		//			throw new NullPointerException("The role root node could not be found.");
		//		}
		//		role = neo4jTemplate.save(role);
		//		root.getRoles().add(role);
		//		neo4jTemplate.save(root);
		//		return role;
		return null;
	}

	@Override
	public Role create(String name) {
		Role role = framedGraph.addVertex(null, Role.class);
		role.setName(name);
		return role;
	}

	@Override
	public RoleRoot createRoot() {
		RoleRoot root = framedGraph.addVertex(null, RoleRoot.class);
		return root;
	}

}
