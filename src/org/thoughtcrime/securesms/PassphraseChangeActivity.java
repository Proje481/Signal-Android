/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import org.thoughtcrime.securesms.crypto.InvalidPassphraseException;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

/**
 * Activity for changing a user's local encryption passphrase.
 *
 * @author Moxie Marlinspike
 */

public class PassphraseChangeActivity extends PassphraseActivity {

  private DynamicTheme    dynamicTheme    = new DynamicTheme();
  private DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private EditText originalPassphrase;
  private EditText newPassphrase;
  private EditText repeatPassphrase;
  private EditText hint;
  private Button   okButton;
  private Button   cancelButton;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
    super.onCreate(savedInstanceState);

    setContentView(R.layout.change_passphrase_activity);

    initializeResources();
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  private void initializeResources() {
    this.originalPassphrase      = (EditText) findViewById(R.id.old_passphrase      );
    this.newPassphrase           = (EditText) findViewById(R.id.new_passphrase      );
    this.repeatPassphrase        = (EditText) findViewById(R.id.repeat_passphrase   );
    this.hint                    = (EditText) findViewById(R.id.passphrase_hint);

    this.okButton                = (Button  ) findViewById(R.id.ok_button           );
    this.cancelButton            = (Button  ) findViewById(R.id.cancel_button       );

    this.okButton.setOnClickListener(new OkButtonClickListener());
    this.cancelButton.setOnClickListener(new CancelButtonClickListener());

    if (TextSecurePreferences.isPasswordDisabled(this)) {
      TextSecurePreferences.setHintText(this, "");
      this.originalPassphrase.setVisibility(View.GONE);
    } else {
      this.originalPassphrase.setVisibility(View.VISIBLE);
    }

    hint.setText(TextSecurePreferences.getHintText(this));
  }

  private String hintControl(String password, String hint) {
    if (hint.length() < 2)
      return getString(R.string.PassphraseChangeActivity_hint_too_short);
    if (hint.length() > 12)
      return getString(R.string.PassphraseChangeActivity_hint_too_big);
    if (hint.contains(password))
      return getString(R.string.PassphraseChangeActivity_hint_contains_pass);
    return null;
  }

  private void verifyAndSavePassphrases() {
    Editable originalText = this.originalPassphrase.getText();
    Editable newText      = this.newPassphrase.getText();
    Editable repeatText   = this.repeatPassphrase.getText();

    String original         = (originalText == null ? "" : originalText.toString());
    String passphrase       = (newText == null ? "" : newText.toString());
    String passphraseRepeat = (repeatText == null ? "" : repeatText.toString());
    String hintText         = (hint.getText() != null ? hint.getText().toString() : "");

    if (TextSecurePreferences.isPasswordDisabled(this)) {
      original = MasterSecretUtil.UNENCRYPTED_PASSPHRASE;
    }

    if (!passphrase.equals(passphraseRepeat)) {
      this.newPassphrase.setText("");
      this.repeatPassphrase.setText("");
      this.newPassphrase.setError(getString(R.string.PassphraseChangeActivity_passphrases_dont_match_exclamation));
      this.newPassphrase.requestFocus();
    } else if (passphrase.equals("")) {
      this.newPassphrase.setError(getString(R.string.PassphraseChangeActivity_enter_new_passphrase_exclamation));
      this.newPassphrase.requestFocus();
    } else {
      String hintCheck = hintControl(passphrase, hintText);
      if (hintCheck != null) {
        this.hint.setError(hintCheck);
        this.hint.requestFocus();
      } else {
        TextSecurePreferences.setHintText(this, hintText);
        new ChangePassphraseTask(this).execute(original, passphrase);
      }
    }
  }

  private class CancelButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      finish();
    }
  }

  private class OkButtonClickListener implements OnClickListener {
    public void onClick(View v) {
      verifyAndSavePassphrases();
    }
  }

  private class ChangePassphraseTask extends AsyncTask<String, Void, MasterSecret> {
    private final Context context;

    public ChangePassphraseTask(Context context) {
      this.context = context;
    }

    @Override
    protected void onPreExecute() {
      okButton.setEnabled(false);
    }

    @Override
    protected MasterSecret doInBackground(String... params) {
      try {
        MasterSecret masterSecret = MasterSecretUtil.changeMasterSecretPassphrase(context, params[0], params[1]);
        TextSecurePreferences.setPasswordDisabled(context, false);

        return masterSecret;

      } catch (InvalidPassphraseException e) {
        Log.w(PassphraseChangeActivity.class.getSimpleName(), e);
        return null;
      }
    }

    @Override
    protected void onPostExecute(MasterSecret masterSecret) {
      okButton.setEnabled(true);

      if (masterSecret != null) {
        setMasterSecret(masterSecret);
      } else {
        originalPassphrase.setText("");
        originalPassphrase.setError(getString(R.string.PassphraseChangeActivity_incorrect_old_passphrase_exclamation));
        originalPassphrase.requestFocus();
      }
    }
  }

  @Override
  protected void cleanup() {
    this.originalPassphrase = null;
    this.newPassphrase      = null;
    this.repeatPassphrase   = null;

    System.gc();
  }
}
