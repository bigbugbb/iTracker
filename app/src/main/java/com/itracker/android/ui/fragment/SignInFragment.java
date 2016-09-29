package com.itracker.android.ui.fragment;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.ServerError;
import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.model.User;
import com.itracker.android.ui.activity.AuthenticatorActivity;
import com.itracker.android.ui.listener.OnAuthStateChangedListener;
import com.itracker.android.ui.listener.OnSignInSignUpSwitchedListener;
import com.itracker.android.utils.AccountUtils;

import org.json.JSONException;
import org.json.JSONObject;

import static com.itracker.android.utils.LogUtils.LOGD;
import static com.itracker.android.utils.LogUtils.makeLogTag;


public class SignInFragment extends Fragment implements OnAuthStateChangedListener {

    private static final String TAG = makeLogTag(SignInFragment.class);
    private static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    private Button mSignIn;
    private TextView mNewAccount;
    private EditText mEmail;
    private EditText mPassword;
    private TextInputLayout mEmailInputLayout;
    private TextInputLayout mPasswordInputLayout;

    private OnSignInSignUpSwitchedListener mSwitchListener;

    public SignInFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false);
    }

    /**
     * Called immediately after {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This gives subclasses a chance to initialize themselves once
     * they know their view hierarchy has been completely created.  The fragment's
     * view hierarchy is not however attached to its parent at this point.
     *
     * @param view               The View returned by {@link #onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmail = (EditText) view.findViewById(R.id.edit_email);
        mPassword = (EditText) view.findViewById(R.id.edit_password);

        mEmailInputLayout = (TextInputLayout) view.findViewById(R.id.input_email);
        mPasswordInputLayout = (TextInputLayout) view.findViewById(R.id.input_password);

        mEmail.addTextChangedListener(new AuthTextWatcher(mEmail));
        mPassword.addTextChangedListener(new AuthTextWatcher(mPassword));

        mSignIn = (Button) view.findViewById(R.id.sign_in);
        mSignIn.setOnClickListener(v -> {
            for (OnAuthStateChangedListener listener
                    : Application.getInstance().getUIListeners(OnAuthStateChangedListener.class)) {
                listener.onAuthStateChanged(AuthState.REGULAR_AUTH_START, null, null);
            }
            signIn();
        });
        mNewAccount = (TextView) view.findViewById(R.id.new_account);
        mNewAccount.setOnClickListener(v -> {
            if (mSwitchListener != null) {
                mSwitchListener.onSwitchToSignUp();
            }
        });

        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);
        final String authTokenType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_AUTHTOKEN_TYPE);
        AccountUtils.setAccountType(Application.getInstance(), accountType);
        AccountUtils.setAuthTokenType(Application.getInstance(), authTokenType);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mSwitchListener = (OnSignInSignUpSwitchedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAccountSignInListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSwitchListener = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Application.getInstance().addUIListener(OnAuthStateChangedListener.class, this);
    }

    @Override
    public void onStop() {
        super.onStop();
        Application.getInstance().removeUIListener(OnAuthStateChangedListener.class, this);
    }

    @Override
    public void onAuthStateChanged(AuthState state, String message, Bundle extra) {
        switch (state) {
            case REGULAR_AUTH_START:
            case GOOGLE_AUTH_START:
                enableUi(false);
                break;
            case REGULAR_AUTH_FAIL:
            case REGULAR_AUTH_SUCCEED:
            case GOOGLE_AUTH_FAIL:
            case GOOGLE_AUTH_SUCCEED:
                enableUi(true);
                break;
        }
    }

    private void signIn() {
        final String email = mEmail.getText().toString().trim();
        final String password = mPassword.getText().toString().trim();
        final String accountType = getActivity().getIntent().getStringExtra(AccountUtils.ARG_ACCOUNT_TYPE);

        if (!validateEmail(email) || !validatePassword(password)) {
            for (OnAuthStateChangedListener listener
                    : Application.getInstance().getUIListeners(OnAuthStateChangedListener.class)) {
                listener.onAuthStateChanged(AuthState.REGULAR_AUTH_FAIL, null, null);
            }
            return;
        }

        Application.getInstance().runInBackground(() -> {
            LOGD(TAG, "Started authenticating");
            Bundle data = new Bundle();
            try {
                User user = AccountUtils.signInUser(email, password);

                data.putString(AccountManager.KEY_ACCOUNT_NAME, email);
                data.putString(AuthenticatorActivity.USERNAME, user.username);
                data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                data.putString(AccountManager.KEY_PASSWORD, password);
                data.putString(AccountManager.KEY_AUTHTOKEN, user.auth_token);

                Bundle userData = new Bundle();
                userData.putString(AccountUtils.USERDATA_USER_ID, user != null ? user.id : null);
                data.putBundle(AccountManager.KEY_USERDATA, userData);
            } catch (Exception e) {
                try {
                    String msg = new String(((ServerError) e.getCause()).networkResponse.data);
                    data.putString(KEY_ERROR_MESSAGE, new JSONObject(msg).getString("errors"));
                } catch (JSONException ex) {
                    data.putString(KEY_ERROR_MESSAGE, "Unknown authenticate error.");
                }
            }

            Application.getInstance().runOnUiThread(() -> {
                String errMsg = data.getString(KEY_ERROR_MESSAGE);
                for (OnAuthStateChangedListener listener
                        : Application.getInstance().getUIListeners(OnAuthStateChangedListener.class)) {
                    if (TextUtils.isEmpty(errMsg)) {
                        listener.onAuthStateChanged(AuthState.REGULAR_AUTH_SUCCEED, null, data);
                    } else {
                        LOGD(TAG, errMsg);
                        listener.onAuthStateChanged(AuthState.REGULAR_AUTH_FAIL, errMsg, null);
                    }
                }
            });
        });
    }

    private void enableUi(boolean enabled) {
        mEmailInputLayout.setEnabled(enabled);
        mPasswordInputLayout.setEnabled(enabled);
        mSignIn.setEnabled(enabled);
        mSignIn.setText(enabled ? R.string.sign_in : R.string.sign_in_user);
        mNewAccount.setEnabled(enabled);
    }

    private boolean validateEmail(String email) {
        if (TextUtils.isEmpty(email) || !isValidEmail(email)) {
            mEmailInputLayout.setError(getString(R.string.invalid_email));
            requestFocus(mEmail);
            return false;
        } else {
            mEmailInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean validatePassword(String password) {
        if (TextUtils.isEmpty(password)) {
            mPasswordInputLayout.setError(getString(R.string.invalid_password));
            requestFocus(mPassword);
            return false;
        } else {
            mPasswordInputLayout.setErrorEnabled(false);
        }
        return true;
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void requestFocus(View view) {
        if (isAdded() && view.requestFocus()) {
            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    private class AuthTextWatcher implements TextWatcher {

        private View mView;

        private AuthTextWatcher(View view) {
            mView = view;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {
            switch (mView.getId()) {
                case R.id.input_email:
                    validateEmail(editable.toString().trim());
                    break;
                case R.id.input_password:
                    validatePassword(editable.toString().trim());
                    break;
            }
        }
    }
}
