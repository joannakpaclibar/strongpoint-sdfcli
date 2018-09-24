package org.strongpoint.sdfcli.plugin.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.json.simple.JSONObject;
import org.strongpoint.sdfcli.plugin.services.DeployCliService;

public class DeployDialog extends TitleAreaDialog{
	
	private Text accountIDText;
	
	private JSONObject results;
	
	private String projectPath;

	public DeployDialog(Shell parentShell) {
		super(parentShell);
	}
	
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	
	public JSONObject getResults() {
		return this.results;
	}
	
	@Override
	public void create() {
		super.create();
		setTitle("Deploy");
		setMessage("Deploy Objects", IMessageProvider.INFORMATION);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        
        createAccountIDElement(container);
        
		return area;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
	}
	
	@Override
	protected Point getInitialSize() {
		return new Point(450, 200);
	}
	
	@Override
	protected void okPressed() {
		System.out.println("[Logger] --- Deploy Dialog OK button is pressed");
		results = DeployCliService.newInstance().deployCliResult(accountIDText.getText(), this.projectPath);
		super.okPressed();
	}
	
	private void createAccountIDElement(Composite container) {
        Label accountIDLabel = new Label(container, SWT.NONE);
        accountIDLabel.setText("Account ID: ");

        GridData accountIDGridData = new GridData();
        accountIDGridData.grabExcessHorizontalSpace = true;
        accountIDGridData.horizontalAlignment = GridData.FILL;

        accountIDText = new Text(container, SWT.BORDER);
        accountIDText.setLayoutData(accountIDGridData);
	}	

}
