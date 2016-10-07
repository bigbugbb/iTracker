package com.itracker.android.ui.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.itracker.android.Application;
import com.itracker.android.R;
import com.itracker.android.data.LogManager;
import com.itracker.android.data.VcardMaps;
import com.itracker.android.data.account.AccountManager;
import com.itracker.android.data.account.OnAccountChangedListener;
import com.itracker.android.data.entity.BaseEntity;
import com.itracker.android.data.extension.vcard.OnVCardListener;
import com.itracker.android.data.extension.vcard.VCardManager;
import com.itracker.android.data.roster.OnContactChangedListener;
import com.itracker.android.xmpp.address.Jid;
import com.itracker.android.xmpp.vcard.AddressProperty;
import com.itracker.android.xmpp.vcard.AddressType;
import com.itracker.android.xmpp.vcard.EmailType;
import com.itracker.android.xmpp.vcard.TelephoneType;
import com.itracker.android.xmpp.vcard.VCardProperty;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ContactVCardViewerFragment extends Fragment implements OnContactChangedListener, OnAccountChangedListener, OnVCardListener {
    public static final String ARGUMENT_ACCOUNT = "com.itracker.android.ui.fragment.ContactVCardViewerFragment.ARGUMENT_ACCOUNT";
    public static final String ARGUMENT_USER = "com.itracker.android.ui.fragment.ContactVCardViewerFragment.ARGUMENT_USER";
    private static final String SAVED_VCARD = "com.itracker.android.ui.fragment.ContactVCardViewerFragment.SAVED_VCARD";
    private static final String SAVED_VCARD_ERROR = "com.itracker.android.ui.fragment.ContactVCardViewerFragment.SAVED_VCARD_ERROR";

    String mAccount;
    String mUser;
    private LinearLayout mXmppItems;
    private LinearLayout mContactInfoItems;
    private VCard mVCard;
    private boolean mVCardError;
    private View mProgressBar;
    private Listener mListener;

    public interface Listener {
        void onVCardReceived();
    }

    public static ContactVCardViewerFragment newInstance(String account, String user) {
        ContactVCardViewerFragment fragment = new ContactVCardViewerFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARGUMENT_ACCOUNT, account);
        arguments.putString(ARGUMENT_USER, user);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mListener = (Listener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mAccount = args.getString(ARGUMENT_ACCOUNT, null);
        mUser = args.getString(ARGUMENT_USER, null);

        mVCard = null;
        mVCardError = false;
        if (savedInstanceState != null) {
            mVCardError = savedInstanceState.getBoolean(SAVED_VCARD_ERROR, false);
            String xml = savedInstanceState.getString(SAVED_VCARD);
            if (xml != null) {
                try {
                    mVCard = parseVCard(xml);
                } catch (XmlPullParserException | IOException | SmackException e) {
                    LogManager.exception(this, e);
                }
            }
        }
    }

    public static VCard parseVCard(String xml) throws XmlPullParserException, IOException, SmackException {
        XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));
        int eventType = parser.next();
        if (eventType != XmlPullParser.START_TAG) {
            throw new IllegalStateException(String.valueOf(eventType));
        }
        if (!VCard.ELEMENT.equals(parser.getName())) {
            throw new IllegalStateException(parser.getName());
        }
        if (!VCard.NAMESPACE.equals(parser.getNamespace())) {
            throw new IllegalStateException(parser.getNamespace());
        }
        return (new VCardProvider()).parse(parser);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_contact_vcard_viewer, container, false);

        mXmppItems = (LinearLayout) view.findViewById(R.id.xmpp_items);
        mContactInfoItems = (LinearLayout) view.findViewById(R.id.contact_info_items);
        mProgressBar = view.findViewById(R.id.contact_info_progress_bar);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Application.getInstance().addUIListener(OnVCardListener.class, this);
        Application.getInstance().addUIListener(OnContactChangedListener.class, this);
        Application.getInstance().addUIListener(OnAccountChangedListener.class, this);

        updateContact(mAccount, mUser);

        if (mVCard == null && !mVCardError) {
            requestVCard();
        } else {
            updateVCard();
        }
    }

    public void requestVCard() {
        mProgressBar.setVisibility(View.VISIBLE);
        VCardManager.getInstance().request(mAccount, mUser);
    }

    @Override
    public void onPause() {
        super.onPause();
        Application.getInstance().removeUIListener(OnVCardListener.class, this);
        Application.getInstance().removeUIListener(OnContactChangedListener.class, this);
        Application.getInstance().removeUIListener(OnAccountChangedListener.class, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_VCARD_ERROR, mVCardError);
        if (mVCard != null) {
            outState.putString(SAVED_VCARD, mVCard.getChildElementXML().toString());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onVCardReceived(String account, String bareAddress, VCard vCard) {
        if (!mAccount.equals(account) || !mUser.equals(bareAddress)) {
            return;
        }
        mVCard = vCard;
        mVCardError = false;
        updateVCard();
        mListener.onVCardReceived();
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onVCardFailed(String account, String bareAddress) {
        if (!mAccount.equals(account) || !mUser.equals(bareAddress)) {
            return;
        }
        mVCard = null;
        mVCardError = true;
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onContactsChanged(Collection<BaseEntity> entities) {
        for (BaseEntity entity : entities) {
            if (entity.equals(mAccount, mUser)) {
                updateContact(mAccount, mUser);
                break;
            }
        }
    }

    @Override
    public void onAccountsChanged(Collection<String> accounts) {
        if (accounts.contains(mAccount)) {
            updateContact(mAccount, mUser);
            if (Jid.getBareAddress(mAccount).equals(Jid.getBareAddress(mUser))) {
                if (AccountManager.getInstance().getAccount(mAccount).getFactualStatusMode().isOnline()) {
                    VCardManager.getInstance().request(mAccount, Jid.getBareAddress(mAccount));
                }
            }
        }
    }

    /**
     * @param source
     * @param value
     * @param splitter
     * @return Concatenated source and value with splitter if necessary.
     */
    private String addString(String source, String value, String splitter) {
        if (value == null || "".equals(value)) {
            return source;
        }
        if (source == null || "".equals(source)) {
            return value;
        }
        return source + splitter + value;
    }

    public VCard getVCard() {
        return mVCard;
    }

    public void updateContact(String account, String bareAddress) {
        mAccount = account;
        mUser = bareAddress;
    }

    public void updateVCard() {
        if (mVCard == null) {
            return;
        }

        mContactInfoItems.removeAllViews();

        addNameInfo(mVCard);

        List<View> birthDayList = new ArrayList<>();
        addItem(birthDayList, mContactInfoItems, getString(R.string.vcard_birth_date), mVCard.getField(VCardProperty.BDAY.toString()));
        addItemGroup(birthDayList, mContactInfoItems, R.drawable.ic_vcard_birthday_24dp, false);

        addOrganizationInfo(mVCard);

        List<View> webList = new ArrayList<>();
        addItem(webList, mContactInfoItems, getString(R.string.vcard_url), mVCard.getField(VCardProperty.URL.toString()));
        addItemGroup(webList, mContactInfoItems, R.drawable.ic_vcard_web_24dp, false);

        addAdditionalInfo(mVCard);
        addAddresses(mVCard);
        addPhones(mVCard);
        addEmails(mVCard);
    }

    private void addEmails(VCard vCard) {
        List<View> emailList = new ArrayList<>();

        String emailHome = vCard.getEmailHome();
        if (!"".equals(emailHome)) {
            addItem(emailList, mContactInfoItems, getString(VcardMaps.getEmailTypeMap().get(EmailType.HOME)), emailHome);
        }

        String emailWork = vCard.getEmailWork();
        if (!"".equals(emailWork)) {
            addItem(emailList, mContactInfoItems, getString(VcardMaps.getEmailTypeMap().get(EmailType.WORK)), emailWork);
        }

        addItemGroup(emailList, mContactInfoItems, R.drawable.ic_vcard_email_24dp, false);
    }

    private void addPhones(VCard vCard) {
        List<View> phoneList = new ArrayList<>();

        for (TelephoneType type : TelephoneType.values()) {
            String types = getString(VcardMaps.getTelephoneTypeMap().get(TelephoneType.HOME));

            String phoneHome = vCard.getPhoneHome(type.name());

            if (!"".equals(phoneHome)) {
                types = addString(types, getString(VcardMaps.getTelephoneTypeMap().get(type)), ", ");
                addItem(phoneList, mContactInfoItems, types, phoneHome);
            }
        }

        for (TelephoneType type : TelephoneType.values()) {
            String types = getString(VcardMaps.getTelephoneTypeMap().get(TelephoneType.WORK));

            String phoneHome = vCard.getPhoneWork(type.name());

            if (!"".equals(phoneHome)) {
                types = addString(types, getString(VcardMaps.getTelephoneTypeMap().get(type)), ", ");
                addItem(phoneList, mContactInfoItems, types, phoneHome);
            }
        }

        addItemGroup(phoneList, mContactInfoItems, R.drawable.ic_vcard_phone_24dp, false);
    }

    private void addAddresses(VCard vCard) {
        List<View> addressList = new ArrayList<>();

        String homeAddress = null;
        for (AddressProperty property : AddressProperty.values()) {
            homeAddress = addString(homeAddress, vCard.getAddressFieldHome(property.name()), "\n");
        }

        addItem(addressList, mContactInfoItems,  getString(VcardMaps.getAddressTypeMap().get(AddressType.HOME)), homeAddress);

        String workAddress = null;
        for (AddressProperty property : AddressProperty.values()) {
            workAddress = addString(workAddress, vCard.getAddressFieldWork(property.name()), "\n");
        }

        addItem(addressList, mContactInfoItems, getString(VcardMaps.getAddressTypeMap().get(AddressType.WORK)), workAddress);

        addItemGroup(addressList, mContactInfoItems, R.drawable.ic_vcard_address_24dp, false);
    }

    private void addAdditionalInfo(VCard vCard) {
        List<View> notesList = new ArrayList<>();
        addItem(notesList, mContactInfoItems, getString(R.string.vcard_note), vCard.getField(VCardProperty.NOTE.name()));
        addItem(notesList, mContactInfoItems, getString(R.string.vcard_decsription), vCard.getField(VCardProperty.DESC.name()));
        addItemGroup(notesList, mContactInfoItems, R.drawable.ic_vcard_notes_24dp, false);
    }

    private void addOrganizationInfo(VCard vCard) {
        List<View> organizationList = new ArrayList<>();

        addItem(organizationList, mContactInfoItems, getString(R.string.vcard_title), vCard.getField(VCardProperty.TITLE.toString()));
        addItem(organizationList, mContactInfoItems, getString(R.string.vcard_role), vCard.getField(VCardProperty.ROLE.toString()));

        String organization = vCard.getOrganization();
        String unit = vCard.getOrganizationUnit();

        addItem(organizationList, mContactInfoItems, getString(R.string.vcard_organization), addString(organization, unit, "\n"));

        addItemGroup(organizationList, mContactInfoItems, R.drawable.ic_vcard_job_title_24dp, false);
    }

    private void addNameInfo(VCard vCard) {
        List<View> nameList = new ArrayList<>();

        addItem(nameList, mContactInfoItems, getString(R.string.vcard_nick_name), vCard.getField(VCardProperty.NICKNAME.name()));
        addItem(nameList, mContactInfoItems, getString(R.string.vcard_formatted_name), vCard.getField(VCardProperty.FN.name()));
        addItem(nameList, mContactInfoItems, getString(R.string.vcard_prefix_name), vCard.getPrefix());

        addItem(nameList, mContactInfoItems, getString(R.string.vcard_given_name), vCard.getFirstName());
        addItem(nameList, mContactInfoItems, getString(R.string.vcard_middle_name), vCard.getMiddleName());
        addItem(nameList, mContactInfoItems, getString(R.string.vcard_family_name), vCard.getLastName());
        addItem(nameList, mContactInfoItems, getString(R.string.vcard_suffix_name), vCard.getSuffix());

        addItemGroup(nameList, mContactInfoItems, R.drawable.ic_vcard_contact_info_24dp, true);
    }

    private void addItemGroup(List<View> nameList, LinearLayout itemList, int groupIcon, boolean firstItemGroup) {
        if (nameList.isEmpty()) {
            return;
        }

        if (!firstItemGroup) {
            addSeparator(itemList);
        }

        ((ImageView) nameList.get(0).findViewById(R.id.contact_info_group_icon)).setImageResource(groupIcon);

        for (View view : nameList) {
            itemList.addView(view);
        }
    }

    private void addItem(List<View> nameList, ViewGroup rootView, String label, String value) {
        View itemView = createItemView(rootView, label, value, null);
        if (itemView != null) {
            Linkify.addLinks((TextView) itemView.findViewById(R.id.contact_info_item_main), Linkify.ALL);
            nameList.add(itemView);
        }
    }

    private void addSeparator(LinearLayout rootView) {
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        rootView.addView(inflater.inflate(R.layout.contact_info_separator, rootView, false));
    }

    private View createItemView(ViewGroup rootView, String label, String value, Integer iconResource) {
        if (value == null || value.isEmpty() ) {
            return null;
        }

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

        View contactInfoItem = inflater.inflate(R.layout.contact_info_item, rootView, false);

        if (label == null || label.trim().isEmpty()) {
            contactInfoItem.findViewById(R.id.contact_info_item_secondary).setVisibility(View.GONE);
        } else {
            ((TextView) contactInfoItem.findViewById(R.id.contact_info_item_secondary)).setText(label);
        }
        ((TextView) contactInfoItem.findViewById(R.id.contact_info_item_main)).setText(value);

        if (iconResource != null) {
            ((ImageView) contactInfoItem.findViewById(R.id.contact_info_group_icon)).setImageResource(iconResource);
        }
        return contactInfoItem;
    }
}