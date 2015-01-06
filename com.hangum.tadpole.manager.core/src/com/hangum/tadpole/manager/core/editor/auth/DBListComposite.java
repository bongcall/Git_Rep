/*******************************************************************************
 * Copyright (c) 2013 hangum.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     hangum - initial API and implementation
 ******************************************************************************/
package com.hangum.tadpole.manager.core.editor.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

import com.hangum.tadpole.commons.exception.dialog.ExceptionDetailsErrorDialog;
import com.hangum.tadpole.commons.google.analytics.AnalyticCaller;
import com.hangum.tadpole.commons.util.ImageUtils;
import com.hangum.tadpole.engine.define.DBDefine;
import com.hangum.tadpole.manager.core.Activator;
import com.hangum.tadpole.manager.core.dialogs.users.FindUserAndDBRoleDialog;
import com.hangum.tadpole.manager.core.editor.executedsql.ExecutedSQLEditor;
import com.hangum.tadpole.manager.core.editor.executedsql.ExecutedSQLEditorInput;
import com.hangum.tadpole.rdb.core.editors.main.MainEditor;
import com.hangum.tadpole.rdb.core.editors.main.MainEditorInput;
import com.hangum.tadpole.rdb.core.viewers.connections.ManagerViewer;
import com.hangum.tadpole.sql.dao.system.TadpoleUserDbRoleDAO;
import com.hangum.tadpole.sql.dao.system.UserDBDAO;
import com.hangum.tadpole.sql.query.TadpoleSystem_UserDBQuery;
import com.hangum.tadpole.sql.query.TadpoleSystem_UserRole;
import com.hangum.tadpole.sql.system.permission.PermissionChecker;
import com.swtdesigner.ResourceManager;

/**
 * 어드민, 메니저, DBA가 사용하는 DB List composite
 * 
 * @author hangum
 *
 */
public class DBListComposite extends Composite {
	/**
	 * Logger for this class
	 */
	private static final Logger logger = Logger.getLogger(DBListComposite.class);

	private TreeViewer treeViewerDBList;
	private List<UserDBDAO> listUserDBs = new ArrayList<UserDBDAO>();
	
	private AdminCompFilter filter;
	private Text textSearch;

	private ToolItem tltmAdd; 
	private ToolItem tltmQueryHistory;
	private ToolItem tltmSQLEditor;
	private ToolItem tltmModify;
	private ToolItem tltmDBDelete;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public DBListComposite(Composite parent, int style) {
		super(parent, style);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 0;
		gridLayout.horizontalSpacing = 0;
		gridLayout.marginHeight = 0;
		gridLayout.marginWidth = 0;
		setLayout(gridLayout);
		
		Composite compositeHead = new Composite(this, SWT.NONE);
		GridLayout gl_compositeHead = new GridLayout(3, false);
		gl_compositeHead.verticalSpacing = 0;
		gl_compositeHead.horizontalSpacing = 0;
		gl_compositeHead.marginHeight = 0;
		gl_compositeHead.marginWidth = 0;
		compositeHead.setLayout(gl_compositeHead);
		compositeHead.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		ToolBar toolBar = new ToolBar(compositeHead, SWT.FLAT | SWT.RIGHT);
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		
		ToolItem tltmRefresh = new ToolItem(toolBar, SWT.NONE);
		tltmRefresh.setImage(ImageUtils.getRefresh());
		tltmRefresh.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				initData();
			}
		});
		tltmRefresh.setToolTipText("Refresh");

		tltmAdd = new ToolItem(toolBar, SWT.NONE);
		tltmAdd.setImage(ImageUtils.getAdd());
		tltmAdd.setEnabled(false);
		tltmAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
				if(ss.isEmpty()) return;
				
				FindUserAndDBRoleDialog dialog = new FindUserAndDBRoleDialog(getShell(), (UserDBDAO)ss.getFirstElement());
				dialog.open();
				initData();
			}
		});
		tltmAdd.setToolTipText("Add User");
		
		tltmDBDelete = new ToolItem(toolBar, SWT.NONE);
		tltmDBDelete.setImage(ImageUtils.getDelete());
		tltmDBDelete.setEnabled(false);
		tltmDBDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteDB();
			}
		});
		tltmDBDelete.setToolTipText("DB Delete");

		tltmModify = new ToolItem(toolBar, SWT.NONE);
		tltmModify.setImage(ImageUtils.getModify());
		tltmModify.setEnabled(false);
		tltmModify.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				modifyDB();
			}
		});
		tltmModify.setToolTipText("Modify");
		
		tltmQueryHistory = new ToolItem(toolBar, SWT.NONE);
		tltmQueryHistory.setImage(ImageUtils.getQueryHistory()); //$NON-NLS-1$
		tltmQueryHistory.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewQueryHistory();
			}
		});
		tltmQueryHistory.setEnabled(false);
		tltmQueryHistory.setToolTipText("Query History");
		
		tltmSQLEditor = new ToolItem(toolBar, SWT.NONE);
		tltmSQLEditor.setImage(ImageUtils.getSQLEditor()); //$NON-NLS-1$
		tltmSQLEditor.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sqlEditor();
			}
		});
		tltmSQLEditor.setEnabled(false);
		tltmSQLEditor.setToolTipText("SQL Editor");
		
		new Label(compositeHead, SWT.NONE);
		
		Label lblSearch = new Label(compositeHead, SWT.NONE);
		lblSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblSearch.setText("Search");
		
		textSearch = new Text(compositeHead, SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		textSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		textSearch.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				filter.setSearchString(textSearch.getText());
				treeViewerDBList.refresh();
			}
		});
		
		Composite compositeBody = new Composite(this, SWT.NONE);
		GridLayout gl_compositeBody = new GridLayout(1, false);
		gl_compositeBody.verticalSpacing = 2;
		gl_compositeBody.horizontalSpacing = 2;
		gl_compositeBody.marginHeight = 2;
		gl_compositeBody.marginWidth = 2;
		compositeBody.setLayout(gl_compositeBody);
		compositeBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		treeViewerDBList = new TreeViewer(compositeBody, SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
		treeViewerDBList.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				
				IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
				if(ss.isEmpty()) return;

				Object objSelect = ss.getFirstElement();
				if(objSelect instanceof UserDBDAO) {
					UserDBDAO userDB = (UserDBDAO)objSelect;
					try {
						List<TadpoleUserDbRoleDAO> listUser = TadpoleSystem_UserRole.getUserList(userDB);
						if(userDB.getListChildren().isEmpty()) {
							userDB.setListChildren(listUser);
							treeViewerDBList.refresh(userDB, true);
							treeViewerDBList.expandToLevel(2);
						}
					} catch (Exception e) {
						logger.error("get user list", e);
					}
					
					toolbarCotrol(PermissionChecker.isDBAdminRole(userDB));
				} else {
					TadpoleUserDbRoleDAO roleDao = (TadpoleUserDbRoleDAO)objSelect;
				}
			}
		});
		treeViewerDBList.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				viewQueryHistory();
			}
		});
		Tree treeAdmin = treeViewerDBList.getTree();
		treeAdmin.setLinesVisible(true);
		treeAdmin.setHeaderVisible(true);
		treeAdmin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		TreeViewerColumn colGroupName = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colGroupName.getColumn().setWidth(150);
		colGroupName.getColumn().setText("Group Name(eMail)");
		
		TreeViewerColumn colRoleName = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colRoleName.getColumn().setWidth(80);
		colRoleName.getColumn().setText("Role");
		
		TreeViewerColumn colEmail = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colEmail.getColumn().setWidth(150);
		colEmail.getColumn().setText("Name");
		
		TreeViewerColumn colName = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colName.getColumn().setWidth(200);
		colName.getColumn().setText("DB Info");
		
		TreeViewerColumn colApproval = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colApproval.getColumn().setWidth(70);
		colApproval.getColumn().setText("User");
		
		TreeViewerColumn colVisible = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colVisible.getColumn().setWidth(40);
		colVisible.getColumn().setText("Visible");
		
		TreeViewerColumn colCreateTime = new TreeViewerColumn(treeViewerDBList, SWT.NONE);
		colCreateTime.getColumn().setWidth(120);
		colCreateTime.getColumn().setText("Create tiem");
		
		treeViewerDBList.setContentProvider(new AdminUserContentProvider());
		treeViewerDBList.setLabelProvider(new AdminUserLabelProvider());
		treeViewerDBList.setInput(listUserDBs);
		initData();
		treeViewerDBList.expandToLevel(2);
		
		filter = new AdminCompFilter();
		treeViewerDBList.addFilter(filter);
		
		// google analytic
		AnalyticCaller.track(this.getClass().getName());
				
	}
	
	/**
	 * toolbar control
	 * 
	 * @param isTrue
	 */
	private void toolbarCotrol(boolean isTrue) {
		tltmAdd.setEnabled(isTrue);
		tltmModify.setEnabled(isTrue);
		
		tltmDBDelete.setEnabled(isTrue);	
		
		tltmQueryHistory.setEnabled(true);
		tltmSQLEditor.setEnabled(true);
	}
	
	private void refreshConnections() {
		final ManagerViewer managerView = (ManagerViewer)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(ManagerViewer.ID);
		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {
				managerView.init();
			}
		});	// end display
	}

	/**
	 * modify db
	 */
	private void modifyDB() {
		IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
		if(ss.isEmpty()) return;
		
		UserDBDAO userDB = (UserDBDAO)ss.getFirstElement();
		
//			if(userDB.getGroup_seq() == SessionManager.getGroupSeq()) {
//				final ModifyDBDialog dialog = new ModifyDBDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), userDB);
//				final int ret = dialog.open();
//				
//				if(ret == Dialog.OK) {
//					treeViewerDBList.setInput(initData());
//					refreshConnections();
//				}
//			}
	}
	
	/**
	 * delete db (delete marking)
	 */
	private void deleteDB(){
		IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
		if(ss.isEmpty()) return;
		
		if(!MessageDialog.openConfirm(null, "Confirm", "Do you want to delete the selected database?") ) return; //$NON-NLS-1$

		UserDBDAO userDB = (UserDBDAO)ss.getFirstElement();
		try {
//				if(userDB.getGroup_seq() == SessionManager.getGroupSeq()) {
//					TadpoleSystem_UserDBQuery.removeUserDB(userDB.getSeq());
//					TadpoleSQLManager.removeInstance(userDB);
//					treeViewerDBList.setInput(initData());
//					refreshConnections();
//				}
		} catch (Exception e) { 
			logger.error("disconnection exception", e);				
			Status errStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e); //$NON-NLS-1$
			ExceptionDetailsErrorDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", "Disconnection Exception", errStatus); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	/**
	 * 사용자가 실행 했던 쿼리의 히스토리를 봅니다.
	 */
	private void viewQueryHistory() {
		IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
		if(ss.isEmpty()) return;
		
		Object objSelect = ss.getFirstElement();
		if(objSelect instanceof UserDBDAO) {
			try {
				ExecutedSQLEditorInput esei = new ExecutedSQLEditorInput((UserDBDAO)ss.getFirstElement());
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(esei, ExecutedSQLEditor.ID, false);
			} catch(Exception e) {
				logger.error("Query History open", e); //$NON-NLS-1$
				
				Status errStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e); //$NON-NLS-1$
				ExceptionDetailsErrorDialog.openError(null, "Error", "Query History", errStatus); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * SQL editor
	 */
	private void sqlEditor() {
		IStructuredSelection ss = (IStructuredSelection)treeViewerDBList.getSelection();
		if(ss.isEmpty()) return;
		
		try {
			MainEditorInput esei = new MainEditorInput((UserDBDAO)ss.getFirstElement());
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(esei, MainEditor.ID, false);
		} catch(Exception e) {
			logger.error("SQL Editor open", e); //$NON-NLS-1$
			
			Status errStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e); //$NON-NLS-1$
			ExceptionDetailsErrorDialog.openError(null, "Error", "SQL Editor", errStatus); //$NON-NLS-1$
		}
	}
	
	/**
	 * 데이터를 초기화 합니다.
	 * 
	 * 1. 사용자 계정 중에 어드민 계정이 있는 지 검색 합니다.
	 * 	1.1) 어드민 계정이 있다면 계정을 표시합니다.
	 * 
	 * @return
	 */
	private void initData() {
		listUserDBs.clear();
		try {
			listUserDBs = TadpoleSystem_UserDBQuery.getUserDB();
			treeViewerDBList.setInput(listUserDBs);
		} catch (Exception e) {
			logger.error("db list", e);
		}
	}
	
	@Override
	protected void checkSubclass() {
	}
}


/**
 * content provider
 * 
 * @author hangum
 *
 */
class AdminUserContentProvider implements ITreeContentProvider {
	private static final Logger logger = Logger.getLogger(AdminUserContentProvider.class);
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement instanceof UserDBDAO) {
			return ((List<UserDBDAO>) inputElement).toArray();
		} else {
			return ((List<TadpoleUserDbRoleDAO>) inputElement).toArray();
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof UserDBDAO) {
			UserDBDAO userDB = (UserDBDAO)parentElement;
			return userDB.getListChildren().toArray();
		}
		return getElements(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		if(element == null) {
			return null;
		}
		
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if(element instanceof ArrayList) {
			return ((ArrayList)element).size() > 0;			
		} else if(element instanceof UserDBDAO) {
			UserDBDAO userDB = (UserDBDAO)element;
			return userDB.getListChildren().size() > 0;
		}
		
		return false;
	}
	
}

/**
 * 유저 정보 레이블 
 * 
 * @author hangum
 *
 */
class AdminUserLabelProvider extends LabelProvider implements ITableLabelProvider {
	private static final Logger logger = Logger.getLogger(AdminUserLabelProvider.class);
	
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		String RDB_CORE_PLUGIN_ID = "com.hangum.tadpole.rdb.core"; //$NON-NLS-1$
		if(element instanceof UserDBDAO) {
			if(columnIndex == 2) {
				DBDefine dbType = ((UserDBDAO)element).getDBDefine();
				if(DBDefine.MYSQL_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/mysql-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.MARIADB_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/mariadb-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.ORACLE_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/oracle-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.SQLite_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/sqlite-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.MSSQL_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/mssql-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.CUBRID_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/cubrid-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.POSTGRE_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/postgresSQL-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.MONGODB_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/mongodb-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.HIVE_DEFAULT == dbType || DBDefine.HIVE2_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/hive-add.png"); //$NON-NLS-1$
				
				else if(DBDefine.TAJO_DEFAULT == dbType) 
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/tajo-add.jpg"); //$NON-NLS-1$
				else
					return ResourceManager.getPluginImage(RDB_CORE_PLUGIN_ID, "resources/icons/database-add.png"); //$NON-NLS-1$
			}
		}
		
		return null;
	}

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if(element instanceof UserDBDAO) {
		
			UserDBDAO userDB = (UserDBDAO)element;
			if(PermissionChecker.isDBAdminRole(userDB)) {
				switch(columnIndex) {
					case 0: return userDB.getGroup_name();
					case 1: return userDB.getRole_id();
					case 2: return userDB.getDisplay_name();
					case 3:
						// sqlite
						if("".equals(userDB.getHost())) return userDB.getUrl();
						return userDB.getHost() + ":"  + userDB.getPort();
					case 4: return userDB.getUsers();
					case 5: return userDB.getIs_visible();
					case 6: return ""+userDB.getCreate_time();
				}
			} else {
				switch(columnIndex) {
				case 0: return userDB.getGroup_name();
					case 1: return userDB.getRole_id();
					case 2: return userDB.getDisplay_name();
					case 3:
						// sqlite
						if("".equals(userDB.getHost())) return userDB.getUrl(userDB.getRole_id());
						return userDB.getHost(userDB.getRole_id()) + ":"  + userDB.getPort(userDB.getRole_id());
					case 4: return userDB.getUsers(userDB.getRole_id());
					case 5: return userDB.getIs_visible();
					case 6: return ""+userDB.getCreate_time();
				}
			}
			
			return "*** not set column ***";
		} else {
			TadpoleUserDbRoleDAO roleDao = (TadpoleUserDbRoleDAO)element;
			switch(columnIndex) {
				case 0: return roleDao.getEmail();
				case 1: return roleDao.getRole_id();
				case 2: return roleDao.getName();
				case 6: return ""+roleDao.getCreate_time();
			}
			
			return "";
		}
		
	}
	
}

/**
 * admin composite filter
 * 
 * @author hangum
 *
 */
class AdminCompFilter extends ViewerFilter {
	String searchString;
	
	public void setSearchString(String s) {
		this.searchString = ".*" + s.toLowerCase() + ".*";
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		
		if(searchString == null || searchString.length() == 0) {
			return true;
		}
		
		UserDBDAO userDB = (UserDBDAO)element;
		if(userDB.getGroup_name().toLowerCase().matches(searchString)) return true;
		if(userDB.getDbms_type().toLowerCase().matches(searchString)) return true;
		if(userDB.getDisplay_name().toLowerCase().matches(searchString)) return true;
		if(userDB.getUrl().toLowerCase().matches(searchString)) return true;
		if(userDB.getHost() != null) if(userDB.getHost().toLowerCase().matches(searchString)) return true;
		if(userDB.getPort() != null)  if(userDB.getPort().toLowerCase().matches(searchString)) return true;
		if((""+userDB.getCreate_time()).toLowerCase().matches(searchString)) return true;
		
		return false;
	}
	
}
