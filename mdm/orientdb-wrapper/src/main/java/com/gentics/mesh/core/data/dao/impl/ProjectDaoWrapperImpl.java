package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.ProjectWrapper;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.router.RouterStorageRegistry;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

// Use ProjectDao instead of ProjectRoot once ready
@Singleton
public class ProjectDaoWrapperImpl extends AbstractDaoWrapper implements ProjectDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(ProjectDaoWrapperImpl.class);

	private final RouterStorageRegistry routerStorageRegistry;

	@Inject
	public ProjectDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions,
		RouterStorageRegistry routerStorageRegistry) {
		super(boot, permissions);
		this.routerStorageRegistry = routerStorageRegistry;
	}

	@Override
	public boolean update(Project project, InternalActionContext ac, EventQueueBatch batch) {
		ProjectUpdateRequest requestModel = ac.fromJson(ProjectUpdateRequest.class);

		String oldName = project.getName();
		String newName = requestModel.getName();
		routerStorageRegistry.assertProjectName(newName);
		if (shouldUpdate(newName, oldName)) {
			// Check for conflicting project name
			Project projectWithSameName = boot.get().projectRoot().findByName(newName);
			if (projectWithSameName != null && !projectWithSameName.getUuid().equals(project.getUuid())) {
				throw conflict(projectWithSameName.getUuid(), newName, "project_conflicting_name");
			}

			project.setName(newName);
			project.setEditor(ac.getUser());
			project.setLastEditedTimestamp();

			// Update the project and its nodes in the index
			batch.add(project.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public TraversalResult<? extends ProjectWrapper> findAll() {
		Stream<? extends Project> nativeStream = boot.get().projectRoot().findAll().stream();
		return new TraversalResult<>(nativeStream.map(ProjectWrapper::new));
	}

	@Override
	public TransformablePage<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		TransformablePage<? extends Project> nativePage = boot.get().projectRoot().findAll(ac, pagingInfo);
		// TODO wrap the page
		return nativePage;
	}

	@Override
	public ProjectWrapper findByName(String name) {
		ProjectRoot root = boot.get().projectRoot();
		Project project = root.findByName(name);
		return ProjectWrapper.wrap(project);
	}

	@Override
	public Project findByName(InternalActionContext ac, String projectName, GraphPermission perm) {
		ProjectRoot root = boot.get().projectRoot();
		Project project = root.findByName(ac, projectName, perm);
		return ProjectWrapper.wrap(project);
	}

	@Override
	public ProjectWrapper findByUuid(String uuid) {
		ProjectRoot root = boot.get().projectRoot();
		Project project = root.findByUuid(uuid);
		return ProjectWrapper.wrap(project);
	}

	@Override
	public Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		ProjectRoot root = boot.get().projectRoot();
		Project project = root.loadObjectByUuid(ac, uuid, perm);
		return ProjectWrapper.wrap(project);
	}

	@Override
	public Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		ProjectRoot root = boot.get().projectRoot();
		Project project = root.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
		return ProjectWrapper.wrap(project);
	}

	@Override
	public Project create(String name, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch) {
		ProjectRoot root = boot.get().projectRoot();

		Project project = root.create();
		if (uuid != null) {
			project.setUuid(uuid);
		}
		project.setName(name);
		project.getNodeRoot();

		// Create the initial branch for the project and add the used schema
		// version to it
		Branch branch = project.getBranchRoot().create(name, creator, batch);
		branch.setMigrated(true);
		if (hostname != null) {
			branch.setHostname(hostname);
		}
		if (ssl != null) {
			branch.setSsl(ssl);
		}
		if (pathPrefix != null) {
			branch.setPathPrefix(pathPrefix);
		} else {
			branch.setPathPrefix("");
		}
		branch.assignSchemaVersion(creator, schemaVersion, batch);

		// Assign the provided schema container to the project
		project.getSchemaContainerRoot().addItem(schemaVersion.getSchemaContainer());
		// project.getLatestBranch().assignSchemaVersion(creator,
		// schemaContainerVersion);
		project.createBaseNode(creator, schemaVersion);

		project.setCreated(creator);
		project.setEditor(creator);
		project.getSchemaContainerRoot();
		project.getTagFamilyRoot();

		root.addItem(project);

		return project;
	}

	@Override
	public Project create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		ProjectRoot projectRoot = boot.get().projectRoot();
		UserDaoWrapper userDao = boot.get().userDao();
		SchemaDaoWrapper schemaDao = boot.get().schemaDao();

		// TODO also create a default object schema for the project. Move this
		// into service class
		// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(,
		// name)
		ProjectCreateRequest requestModel = ac.fromJson(ProjectCreateRequest.class);
		String projectName = requestModel.getName();
		MeshAuthUser creator = ac.getUser();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "project_missing_name");
		}
		if (!userDao.hasPermission(creator, projectRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		// TODO instead of this check, a constraint in the db should be added
		Project conflictingProject = projectRoot.findByName(requestModel.getName());
		if (conflictingProject != null) {
			throw new NameConflictException("project_conflicting_name", projectName, conflictingProject.getUuid());
		}
		routerStorageRegistry.assertProjectName(requestModel.getName());

		if (requestModel.getSchema() == null || !requestModel.getSchema().isSet()) {
			throw error(BAD_REQUEST, "project_error_no_schema_reference");
		}
		SchemaVersion schemaVersion = schemaDao.fromReference(requestModel.getSchema());

		String hostname = requestModel.getHostname();
		Boolean ssl = requestModel.getSsl();
		String pathPrefix = requestModel.getPathPrefix();
		Project project = create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, uuid, batch);
		Branch initialBranch = project.getInitialBranch();
		String branchUuid = initialBranch.getUuid();

		// Add project permissions
		userDao.addCRUDPermissionOnRole(creator, projectRoot, CREATE_PERM, project);
		userDao.inheritRolePermissions(creator, project, project.getBaseNode());
		userDao.inheritRolePermissions(creator, project, project.getTagFamilyRoot());
		userDao.inheritRolePermissions(creator, project, project.getSchemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, project.getMicroschemaContainerRoot());
		userDao.inheritRolePermissions(creator, project, project.getNodeRoot());
		userDao.inheritRolePermissions(creator, project, initialBranch);

		// Store the project and the branch in the index
		batch.add(project.onCreated());
		batch.add(initialBranch.onCreated());

		// Add events for created basenode
		batch.add(project.getBaseNode().onCreated());
		project.getBaseNode().getDraftGraphFieldContainers().forEach(c -> {
			batch.add(c.onCreated(branchUuid, DRAFT));
		});

		return project;

	}

	@Override
	public void delete(Project project, BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting project {" + project.getName() + "}");
		}

		// Remove the nodes in the project hierarchy
		Node base = project.getBaseNode();
		base.delete(bac, true, true);

		// Remove the tagfamilies from the index
		project.getTagFamilyRoot().delete(bac);

		// Remove all nodes in this project
		for (Node node : project.findNodes()) {
			node.delete(bac, true, false);
			bac.inc();
		}

		// Finally also remove the node root
		project.getNodeRoot().delete(bac);

		// Unassign the schema from the container
		for (Schema container : project.getSchemaContainerRoot().findAll()) {
			project.getSchemaContainerRoot().removeSchemaContainer(container, bac.batch());
		}

		// Remove the project schema root from the index
		project.getSchemaContainerRoot().delete(bac);

		// Remove the branch root and all branches
		project.getBranchRoot().delete(bac);

		// Remove the project from the index
		bac.add(project.onDeleted());

		// Finally remove the project node
		project.getVertex().remove();

		bac.process(true);

	}

	@Override
	public ProjectResponse transformToRestSync(Project project, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		ProjectResponse restProject = new ProjectResponse();
		if (fields.has("name")) {
			restProject.setName(project.getName());
		}
		if (fields.has("rootNode")) {
			restProject.setRootNode(project.getBaseNode().transformToReference(ac));
		}

		project.fillCommonRestFields(ac, fields, restProject);
		setRolePermissions(project, ac, restProject);

		return restProject;

	}

}