package nus.ee2001.smartfridge;

import nus.ee2001.smartfridge.R;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.text.InputType;
import android.view.View;
import android.telephony.SmsManager;
import android.content.Intent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This is the main activity of the HelloIOIO example application.
 * 
 * It displays a toggle button on the screen, which enables control of the
 * on-board LED. This example shows a very simple usage of the IOIO, by using
 * the {@link AbstractIOIOActivity} class. For a more advanced use case, see the
 * HelloIOIOPower example.
 */
public class MainActivity extends AbstractIOIOActivity {
	
	public enum SyncState {
		IDLE, SENQ, SCMD, SACK, SDAT
	}
	
	public enum SyncCmd {
		NIL, GROCERY, RECIPE
	}
	
	protected Button btn_sync, btn_save, btn_send, btn_reset;
	protected Spinner sp_recipe;
	protected CheckBox[] checks;
	protected EditText tb_name, low_grocs, groc_dest;
	protected RadioGroup mode_grp;
	protected RadioButton mode_sms, mode_email;
	protected SyncState sync_state;
	protected SyncCmd sync_cmd;
	protected int data_bytes, cur_mode;
	protected boolean reset = false;
	
	protected final byte kENQ = 0x05;
	protected final byte kACK = 0x06;
	protected final byte kNAK = 0x15;
	protected final byte kEOT = 0x04;
	protected final byte kDC1 = 0x11;
	protected final byte kDC2 = 0x12;
	protected final byte kSYN = 0x16;
	
	
	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		btn_sync = (Button) findViewById(R.id.startsyncb);
		btn_reset = (Button) findViewById(R.id.resetb);
		btn_save = (Button) findViewById(R.id.saveb);
		low_grocs = (EditText) findViewById(R.id.low_grocs);
		mode_grp = (RadioGroup) findViewById(R.id.mode_grp);
		mode_sms = (RadioButton) findViewById(R.id.mode_sms);
		mode_email = (RadioButton) findViewById(R.id.mode_email);
		groc_dest = (EditText) findViewById(R.id.groc_dest);
		btn_send = (Button) findViewById(R.id.sendb);
		sp_recipe = (Spinner) findViewById(R.id.recipesel);
		checks = new CheckBox[5];
		checks[0] = (CheckBox) findViewById(R.id.cb_milk);
		checks[1] = (CheckBox) findViewById(R.id.cb_egg);
		checks[2] = (CheckBox) findViewById(R.id.cb_fruit);
		checks[3] = (CheckBox) findViewById(R.id.cb_veg);
		checks[4] = (CheckBox) findViewById(R.id.cb_choc);
		tb_name = (EditText) findViewById(R.id.rec_name);
		
		mode_grp.check(R.id.mode_sms);
		cur_mode = mode_grp.getCheckedRadioButtonId();
		groc_dest.setHint(R.string.hint_sms);
		groc_dest.setInputType(InputType.TYPE_CLASS_PHONE);
		
		btn_sync.setEnabled(false);
		low_grocs.setEnabled(false);
		mode_sms.setEnabled(false);
		mode_email.setEnabled(false);
		groc_dest.setEnabled(false);
		btn_send.setEnabled(false);
		sp_recipe.setEnabled(false);
		tb_name.setEnabled(false);
		btn_save.setEnabled(false);
		for (int i = 0; i < 5; ++i) {
			checks[i].setEnabled(false);
		}
		
		sync_state = SyncState.IDLE;
		data_bytes = 0;
	}
	
	public void myToast(String str) {
		Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
	}
	
	public void buttonPressed(View button) {
		String btag = (String) button.getTag();
		if (btag == "StartSync") {
			sync_cmd = SyncCmd.GROCERY;
			sync_state = SyncState.SENQ;
		} else if (btag == "Save") {
			
		} else if (btag == "SendSMS") {
			
		} else if (btag == "SendEmail") {
			
		}
	}
	
	public void startSync(View btn) {
		if (sync_state == SyncState.IDLE) {
			sync_cmd = SyncCmd.GROCERY;
			sync_state = SyncState.SENQ;
		} else {
			Toast.makeText(getApplicationContext(), "Sync in progress.", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void saveRecipe(View btn) {
		if (sync_state == SyncState.IDLE) {
			sync_cmd = SyncCmd.RECIPE;
			sync_state = SyncState.SENQ;
		} else {
			Toast.makeText(getApplicationContext(), "Sync in progress.", Toast.LENGTH_SHORT).show();
		}
	}
	
	public void resetButton(View btn) {
		reset = true;
	}
	
	public void modeSwitch(View btn) {
		int new_mode = mode_grp.getCheckedRadioButtonId();
		if (new_mode == cur_mode) {
			return;
		}
		
		if (new_mode == R.id.mode_sms) {
			groc_dest.setInputType(InputType.TYPE_CLASS_PHONE);
			groc_dest.setHint(R.string.hint_sms);
			groc_dest.setText("");
		} else if (new_mode == R.id.mode_email) {
			groc_dest.setInputType(InputType.TYPE_CLASS_TEXT
								 | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
			groc_dest.setHint(R.string.hint_email);
			groc_dest.setText("");
		}
		cur_mode = new_mode;
	}
	
	public void sendList(View btn) {
		String dest = groc_dest.getText().toString();
		String dest2[] = { dest };
		String lowstuff = low_grocs.getText().toString();
		if (mode_sms.isChecked()) {
			SmsManager manager = SmsManager.getDefault();
			manager.sendTextMessage(dest, null, lowstuff, null, null);
			myToast("Text message sent!");
		} else if (mode_email.isChecked()) {
			final Intent email = new Intent(Intent.ACTION_SEND);
			email.setType("message/rfc822");
			email.putExtra(Intent.EXTRA_EMAIL, dest2);
			email.putExtra(Intent.EXTRA_SUBJECT, "Grocery List");
			email.putExtra(Intent.EXTRA_TEXT, lowstuff);
			try {
				//startActivity(Intent.createChooser(email, "Send Email"));
				startActivity(email);
			} catch (android.content.ActivityNotFoundException anfe) {
				myToast("No Email clients installed.");
			} catch (Exception e) {
				myToast("ROFL");
			}
		}
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		/** The on-board LED. */
		private DigitalOutput led_stat;
		private Uart sync_uart;
		private InputStream sync_in;
		private OutputStream sync_out;
		
		private byte inp_buf;
		private boolean sent;
		private long thread_sleep;
		private int linkstate = 0;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led_stat = ioio_.openDigitalOutput(IOIO.LED_PIN);
			DigitalInput.Spec uart_rx = new DigitalInput.Spec(4, DigitalInput.Spec.Mode.FLOATING);
			DigitalOutput.Spec uart_tx = new DigitalOutput.Spec(3, DigitalOutput.Spec.Mode.OPEN_DRAIN);
			sync_uart = ioio_.openUart(uart_rx, uart_tx, 33600, Uart.Parity.NONE, Uart.StopBits.ONE);
			sync_in = sync_uart.getInputStream();
			sync_out = sync_uart.getOutputStream();
			
			led_stat.write(true);
		}
		
		protected void uiEnable(int iid) {
			final int id = iid;
			runOnUiThread(new Runnable() {
				public void run() {
					View iv = (View) findViewById(id);
					iv.setEnabled(true);
				}
			});
		}
		
		protected void uiDisable(int iid) {
			final int id = iid;
			runOnUiThread(new Runnable() {
				public void run() {
					View iv = (View) findViewById(id);
					iv.setEnabled(false);
				}
			});
		}
		
		protected void uiGroceries(byte inp) {
			boolean low[] = new boolean[5];
			low[0] = (inp & 0x01) == 0x01;
			low[1] = (inp & 0x02) == 0x02;
			low[2] = (inp & 0x04) == 0x04;
			low[3] = (inp & 0x08) == 0x08;
			low[4] = (inp & 0x10) == 0x10;
			String names[] = {"Milk", "Eggs", "Fruit", "Vegetables", "Chocolates"};
			boolean firstlow = true;
			String tmp = "Groceries to buy:";
			for (int i = 0; i < 5; ++i) {
				if (!low[i]) {
					if (!firstlow) {
						tmp += ",";
					}
					tmp += " ";
					tmp += names[i];
					firstlow = false;
				}
			}
			
			final String grocstr = tmp;
			runOnUiThread(new Runnable() {
				public void run() {
					low_grocs.setText(grocstr);
					low_grocs.setEnabled(true);
					mode_sms.setEnabled(true);
					mode_email.setEnabled(true);
					groc_dest.setEnabled(true);
					btn_send.setEnabled(true);
				}
			});
		}
		
		protected void uiToast(CharSequence instr) {
			final CharSequence str = instr;
			runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
				}
			});
		}
		
		protected int syncProtocol() throws ConnectionLostException, IOException {
			int sleeptime = 10;
			
			if (sync_state == SyncState.IDLE) {
				sleeptime = 100;
				sync_out.write(kSYN);
				if (sync_in.available() > 0) {
					linkstate = 20;
					inp_buf = (byte) sync_in.read();
				}
				sent = false;
			} else if (sync_state == SyncState.SENQ) {
				led_stat.write(false);
				if (!sent) {
					sync_out.write(kENQ);
					sent = true;
				}
				if (sync_in.available() > 0) {
					linkstate = 20;
					inp_buf = (byte) sync_in.read();
					if (inp_buf == kACK && sync_in.available() == 0) {
						sent = false;
						sync_state = SyncState.SCMD;
					}
				}
			} else if (sync_state == SyncState.SCMD && sync_cmd != SyncCmd.NIL) {
				if (sync_cmd == SyncCmd.GROCERY) {
					if (!sent) {
						sent = true;
						sync_out.write(kDC1);
					}
					if (sync_in.available() > 1) {
						linkstate = 20;
						inp_buf = (byte) sync_in.read();
					}
					inp_buf = (byte) sync_in.read();
					inp_buf &= 0x1F; // extract lowest 5 bits
					uiGroceries(inp_buf);
					sync_cmd = SyncCmd.NIL;
					sync_state = SyncState.SACK;
				} else if (sync_cmd == SyncCmd.RECIPE) {
					sync_out.write(kDC2);
					uiToast("DC2 Sent");
					while (sync_in.available() > 0) {
						uiToast("WACK0");
						linkstate = 20;
						inp_buf = (byte) sync_in.read();
						if (inp_buf == kDC2) {
							uiToast("DC2 echo recv");
							sync_cmd = SyncCmd.NIL;
							sync_state = SyncState.SDAT;
						}
					}
				}
			} else if (sync_state == SyncState.SDAT) {
				uiToast("Mode SDAT");
				linkstate = 20;
				byte eep_addr = (byte) (sp_recipe.getSelectedItemPosition() * 6);
				byte temp = 0;
				for (int i = 0; i < 5; ++i) {
					if (checks[i].isChecked()) {
						temp |= (0x01 << i);
					}
				}
				inp_buf = 0x00;
				sync_out.write(eep_addr);
				while (inp_buf != kACK) {
					while (sync_in.available() < 1);
					uiToast("Dumping packets till ACK1");
					inp_buf = (byte) sync_in.read();
				}
				inp_buf = 0x00;
				sync_out.write(temp);
				while (inp_buf != kACK) {
					while (sync_in.available() < 1);
					uiToast("Dumping packets till ACK2");
					inp_buf = (byte) sync_in.read();
				}
				for (int i = 0; i < 5; ++i) {
					inp_buf = 0x00;
					sync_out.write(tb_name.getText().charAt(i));
					uiToast("Writing DAT");
					while (inp_buf != kACK) {
						uiToast("WACK3");
						while (sync_in.available() < 1);
						uiToast("ACK3");
						inp_buf = (byte) sync_in.read();
					}
				}
				sync_state = SyncState.SACK;
			} else if (sync_state == SyncState.SACK) {
				uiToast("EoT!");
				sync_out.write(kACK);
				while (sync_in.available() > 0) {
					linkstate = 10;
					inp_buf = (byte) sync_in.read();
					if (inp_buf == kEOT) {
						led_stat.write(true);
						sync_state = SyncState.IDLE;
					}
				}
			}
			if (linkstate == 20) {
				uiEnable(R.id.startsyncb);
				uiEnable(R.id.recipesel);
				uiEnable(R.id.rec_name);
				uiEnable(R.id.cb_milk);
				uiEnable(R.id.cb_egg);
				uiEnable(R.id.cb_fruit);
				uiEnable(R.id.cb_veg);
				uiEnable(R.id.cb_choc);
				uiEnable(R.id.saveb);
			} else if (linkstate == 0) {
				sync_state = SyncState.IDLE;
				sync_cmd = SyncCmd.NIL;
				uiDisable(R.id.startsyncb);
				uiDisable(R.id.recipesel);
				uiDisable(R.id.rec_name);
				uiDisable(R.id.cb_milk);
				uiDisable(R.id.cb_egg);
				uiDisable(R.id.cb_fruit);
				uiDisable(R.id.cb_veg);
				uiDisable(R.id.cb_choc);
				uiDisable(R.id.saveb);
			}
			if (linkstate > 0) {
				--linkstate;
			}
			return sleeptime;
		}
		
		
		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		protected void loop() throws ConnectionLostException {
			try {
				if (reset) {
					reset = false;
					ioio_.hardReset();
				}
				thread_sleep = syncProtocol();
				sleep(thread_sleep);
			} catch (InterruptedException e) {
			} catch (IOException ioe) {
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
}