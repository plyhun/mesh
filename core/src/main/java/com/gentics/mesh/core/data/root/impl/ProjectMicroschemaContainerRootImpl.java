package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.event.Assignment.ASSIGNED;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Project specific implementation of microschema container root.
 */
public class ProjectMicroschemaContainerRootImpl extends MicroschemaContainerRootImpl {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(ProjectMicroschemaContainerRootImpl.class, MeshVertexImpl.class);
	}

	/**
	 * Get the project.
	 * 
	 * @return project
	 */
	protected HibProject getProject() {
		return in(HAS_MICROSCHEMA_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void addMicroschema(HibUser user, HibMicroschema microschema, EventQueueBatch batch) {
		Project project = toGraph(getProject());
		batch.add(project.onMicroschemaAssignEvent(microschema, ASSIGNED));
		super.addMicroschema(user, microschema, batch);

		// assign the latest schema version to all branches of the project
		for (Branch branch : project.getBranchRoot().findAll()) {
			branch.assignMicroschemaVersion(user, microschema.getLatestVersion(), batch);
		}
	}

	@Override
	public void removeMicroschema(HibMicroschema microschema, EventQueueBatch batch) {
		Project project = toGraph(getProject());
		batch.add(project.onMicroschemaAssignEvent(microschema, UNASSIGNED));
		super.removeMicroschema(microschema, batch);

		// unassign the schema from all branches
		for (Branch branch : project.getBranchRoot().findAll()) {
			branch.unassignMicroschema(microschema);
		}
	}
}
