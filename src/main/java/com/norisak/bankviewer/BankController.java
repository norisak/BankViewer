package com.norisak.bankviewer;

import com.norisak.bankviewer.network.geapi.GeApi;
import com.norisak.bankviewer.network.geapi.GeApiRequestIcon;
import com.norisak.bankviewer.network.PacketCapturer;
import com.norisak.bankviewer.network.PacketHandler;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.pcap4j.core.*;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;

public class BankController {

	private final ByteArrayDecoder decoder;

	private final ItemDataOracle itemDataOracle;

	private PacketListener packetListener;

	private String lastPath;

	@FXML
	private TextArea wiresharkHexField;

	@FXML
	private Button captureButton;

	@FXML
	private Button saveButton;

	@FXML
	private Button loadButton;

	@FXML
	private Button compareButton;

	@FXML
	private TextArea bankTextArea;

	@FXML
	private ScrollPane itemScrollPane;

	@FXML
	private VBox itemVbox;  //TODO: Use ListView instead of VBox for allegedly better performance

	@FXML
	private ListView<AnchorPane> listView;

	@FXML
	private Label captureStatusLabel;

	@FXML
	private Label totalWealthLabel;

	@FXML
	private RadioButton radioButtonOrderByStackValue;

	@FXML
	private RadioButton radioButtonOrderByStackSize;

	@FXML
	private RadioButton radioButtonOrderByItemValue;

	@FXML
	private RadioButton radioButtonOrderByNatural;

	@FXML
	private CheckBox reverseOrderCheckbox;

	@FXML
	private ToggleGroup sortToggleGroup;

	private HashMap<String, Comparator<ItemStack>> itemStackComparators;

	private Comparator<ItemStack> selectedComparator;

	private PacketCapturer capturer;
	private PacketHandler handler;

	private Thread packetWorker = null;
	private Bank displayedBank;

	private long bankValue;
	private boolean isCompare;


	/**
	 * TODO: (MVP):
	 *
	 * Automatically update prices, and detect API failures
	 * Item Database feature
	 * Click on item stack in bank viewer for details
	 *
	 * TODO: (Nice features)
	 *
	 * Remove JSON dependency by encoding our own binary file
	 * Multiple themes (Legacy, RS3, maybe plain white?)
	 */

	public BankController(){

		itemDataOracle = new ItemDataOracle();

		decoder = new ByteArrayDecoder(this);

		handler = new PacketHandler(decoder, this);
		handler.generateIpWhitelist();

		itemStackComparators = new HashMap<>();


		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// TODO: Make these shorter
				itemStackComparators.put(radioButtonOrderByStackValue.getId(), new Comparator<ItemStack>() {
					@Override
					public int compare(ItemStack o1, ItemStack o2) {
						long diff = o1.getValue(itemDataOracle) - o2.getValue(itemDataOracle);
						if (diff < 0)
							return 1;
						if (diff == 0)
							return 0;
						return -1;
					}
				});

				itemStackComparators.put(radioButtonOrderByItemValue.getId(), new Comparator<ItemStack>() {
					@Override
					public int compare(ItemStack o1, ItemStack o2) {
						long diff = itemDataOracle.getValueFromId(o1.getItemId()) - itemDataOracle.getValueFromId(o2.getItemId());
						if (diff < 0)
							return 1;
						if (diff == 0)
							return 0;
						return -1;
					}
				});

				itemStackComparators.put(radioButtonOrderByStackSize.getId(), new Comparator<ItemStack>() {
					@Override
					public int compare(ItemStack o1, ItemStack o2) {
						long diff = o1.getItemCount() - o2.getItemCount();
						if (diff < 0)
							return 1;
						if (diff == 0)
							return 0;
						return -1;
					}
				});

				selectedComparator = itemStackComparators.get(radioButtonOrderByStackValue.getId());
				}
		});


		try {
			capturer = new PacketCapturer(handler);
		} catch (Throwable e){  // If WinPcap / libpcap isn't installed
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					captureStatusLabel.setText("WinPcap / libpcap not installed.\nCapture unavailable.");
					captureButton.setDisable(true);
				}
			});
		}

		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				addItemStackToVBox(new ItemStack(995, 27, new long[]{}));
			}
		});
	}

	public void onSortSettingsChanged(ActionEvent event){

		String selectedButton = ((RadioButton) sortToggleGroup.getSelectedToggle()).getId();

		selectedComparator = itemStackComparators.get(selectedButton);

		if (displayedBank != null){
			reSortBank();
		}
	}

	public void onPressCapture(ActionEvent event){

		captureButton.setDisable(true);

		long before = System.nanoTime();

		captureStatusLabel.setTextFill(Color.BLACK);
		captureStatusLabel.setText("Listening for runescape connection..");

		try {
			capturer.start();
		} catch (PcapNativeException e) {
			e.printStackTrace();
			System.out.println("lol");
		}

		packetWorker = new Thread(){
			@Override
			public void run(){
				handler.workOnPackets();
			}
		};

		packetWorker.start();

		System.out.println(System.nanoTime()-before);
	}

	public void onPressCompare(ActionEvent event){
		Bank bank = selectBankFromFile();

		if (bank == null){
			return;
		}


		bank = displayedBank.getDelta(bank);

		handleBank(bank, true);
		saveButton.setDisable(true);
	}

	public void onPressSave(ActionEvent event){

		if (displayedBank.save()){
			saveButton.setDisable(true);
		}
		// TODO: Show an error message if it fails

	}

	public void onPressLoad(ActionEvent event){
		Bank bank = selectBankFromFile();

		if (bank != null){
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					handleBank(bank, false);
					saveButton.setDisable(true);
				}
			});
		}
	}

	/**
	 * Adds the itemstack to the Vbox list view thing.
	 * @param itemStack
	 * @return The value of the itemStack added.
	 */
	public long addItemStackToVBox(ItemStack itemStack){
		AnchorPane pane = new AnchorPane();
		pane.setId("" + itemStack.getItemId());
		pane.setPrefWidth(itemVbox.getWidth()-12);
		pane.setMinHeight(32);
		pane.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, CornerRadii.EMPTY,
				BorderWidths.DEFAULT)));

		Label itemNameLabel = new Label(itemDataOracle.getNameFromId(itemStack.getItemId()));
		Label countLabel = new Label(String.format("%,d", itemStack.getItemCount()) + " x");
		long value = itemStack.getValue(itemDataOracle);
		Label gpLabel = new Label(String.format("%,d", value) + " gp");

		AnchorPane.setLeftAnchor(itemNameLabel, 40.0);
		AnchorPane.setBottomAnchor(itemNameLabel, 0.0);

		AnchorPane.setLeftAnchor(countLabel, 40.0);
		AnchorPane.setTopAnchor(countLabel, 0.0);

		AnchorPane.setRightAnchor(gpLabel, 25.0);
		AnchorPane.setTopAnchor(gpLabel, 10.0);


		// Load the icon if it exists

		Image img = IconManager.getInstance().loadIconIfExists(itemStack.getItemId());

		// countLabel and gpLabel must be in position 1 and 2 due to hard coded updatePrice method.
		pane.getChildren().addAll(itemNameLabel, countLabel, gpLabel);

		if (img != null){
			ImageView imgView = new ImageView(img);

			AnchorPane.setLeftAnchor(imgView, 0.0);
			AnchorPane.setTopAnchor(imgView, 0.0);

			pane.getChildren().add(imgView);
		} else{
			GeApi.queueRequest(new GeApiRequestIcon(itemStack.getItemId(), false, itemDataOracle, this));
		}




		itemVbox.getChildren().add(pane);

		return value;
	}

	/**
	 * Shows a file selection interface that allows the user to select a bank, and returns the resulting bank object.
	 * @return A bank object, or null if the method failed
	 */
	public Bank selectBankFromFile(){
		FileChooser chooser = new FileChooser();
		chooser.setInitialDirectory(new File("banks"));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("rsbank files (*.rsbank)", "*.rsbank"));
		File file = chooser.showOpenDialog(Main.stage);

		if (file == null){
			System.out.println("No file selected");
			return null;
		}


		System.out.println("Opened file "+file.getAbsolutePath());


		return Bank.load(file);
	}

	public void reSortBank(){
		itemVbox.getChildren().clear();
		displayedBank.sort(itemDataOracle, selectedComparator, reverseOrderCheckbox.isSelected());
		displayedBank.fillVbox(this, isCompare);
	}

	/**
	 * Called when we successfully decoded a Bank object from the captured network traffic
	 * @param bank
	 */
	public void handleBank(Bank bank, boolean isCompare){
		this.isCompare = isCompare;


		if (capturer.isCapturing()){
			capturer.stop();
			handler.stopWorking();
			packetWorker.interrupt();
		}


		displayedBank = bank;



		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				itemVbox.getChildren().clear();
				captureStatusLabel.setText("");
			}
		});

		captureButton.setDisable(false);
		saveButton.setDisable(false);
		compareButton.setDisable(isCompare);
		System.out.println(selectedComparator);
		bank.sort(itemDataOracle, selectedComparator, reverseOrderCheckbox.isSelected());
		bank.fillVbox(this, isCompare);

		bank.queuePriceUpdates(itemDataOracle,this);


	}

	public Label getTotalWealthLabel(){
		return totalWealthLabel;
	}

	public Label getCaptureStatusLabel(){
		return captureStatusLabel;
	}

	public long getBankValue() {
		return bankValue;
	}

	public void setBankValue(long bankValue) {
		this.bankValue = bankValue;
	}

	public long parseLabelNumberFromNode(Label l){
		// Get only the digits so we can parse the value.
		StringBuilder b = new StringBuilder();

		for (char c : l.getText().toCharArray()){
			if (Character.isDigit(c) || c == '-'){
				b.append(c);
			}
		}

		return Long.parseLong(b.toString());
	}

	public void updatePrice(int itemId, long newPrice){
		Platform.runLater(new Runnable() {
			@Override
			public void run() {  // Here we go...
				if (itemVbox != null && itemVbox.getChildren() != null)
					for(Node node : itemVbox.getChildren()){
						if (node instanceof AnchorPane) {
							AnchorPane pane = (AnchorPane) node;
							if (pane.getId().equals("" + itemId)){

								// Hard coded positions in the children list
								Label valueLabel = (Label) pane.getChildren().get(2);
								Label countLabel = (Label) pane.getChildren().get(1);
								System.out.println(valueLabel.getText());

								// Subtract the old value of this itemStack from the bank value
								bankValue -= parseLabelNumberFromNode(valueLabel);

								int itemCount = (int) parseLabelNumberFromNode(countLabel);

								long value = itemCount * newPrice;

								// Add the new value of the itemStack to the bank value
								bankValue += value;

								if (getTotalWealthLabel().getText().contains("difference")){
									getTotalWealthLabel().setText("Wealth difference:   " + String.format("%,d", bankValue) + " gp");
								}
								else{
									getTotalWealthLabel().setText("Total wealth:   " + String.format("%,d", bankValue) + " gp");
								}

								valueLabel.setText(String.format("%,d", value) + " gp");

							}
						}
					}
			}
		});
	}

	public void updateIcon(int itemId) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if (itemVbox != null && itemVbox.getChildren() != null)
					for(Node node : itemVbox.getChildren()){
						if (node instanceof AnchorPane) {
							AnchorPane pane = (AnchorPane) node;
							if (pane.getId().equals("" + itemId)){
								Image img = IconManager.getInstance().loadIconIfExists(itemId);

								if (img != null){
									ImageView imgView = new ImageView(img);

									AnchorPane.setLeftAnchor(imgView, 0.0);
									AnchorPane.setTopAnchor(imgView, 0.0);

									pane.getChildren().add(imgView);
								}
							}
						}
					}
			}
		});

	}
}
