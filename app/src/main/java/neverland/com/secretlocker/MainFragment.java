package neverland.com.secretlocker;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainFragment extends Fragment implements PasswordFragment.PasswordDialogListener {

    private static final int MSG_DONE = 0;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_DONE:
                    mEncryptButton.setEnabled(true);
                    mDecryptButton.setEnabled(true);
                    mProgressDialog.dismiss();
                    Toast.makeText(getActivity(), getString(R.string.toast_message), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private String mMode;
    private View mView;
    private File[] mSubFiles;
    private ListView mListView;
    private SimpleAdapter mSimpleAdapter;
    private ArrayList<HashMap<String, String>> mFilesArrayList;
    private ListView.OnItemClickListener onItemClick = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            HashMap<String, String> list = (HashMap) mSimpleAdapter.getItem(position);

            if (list.get("Type").equals(getString(R.string.file_type_directory))) {
                listFiles(new File(list.get("Path")));
            } else {
                mFile = new File(list.get("Path"));

                mPasswordFragment.show(getFragmentManager(), "");
            }
        }
    };

    private Button mEncryptButton;
    private Button mDecryptButton;

    private FileInputStream mFileInputStream;
    private FileOutputStream mFileOutputStream;
    private File mFile;
    private Cipher mCipher;
    private SecretKeySpec mSecretKeySpec;
    private IvParameterSpec mIvParameterSpec;
    private CipherOutputStream mCipherOutputStream;
    private CipherInputStream mCipherInputStream;

    private ProgressDialog mProgressDialog;

    private EncryptThread mEncryptThread;

    private class EncryptThread extends Thread {
        @Override
        public void run() {
            super.run();
            encrypt();

            mHandler.sendEmptyMessage(MSG_DONE);
        }
    }

    private DecryptThread mDecryptThread;

    private class DecryptThread extends Thread {
        @Override
        public void run() {
            super.run();
            decrypt();

            mHandler.sendEmptyMessage(MSG_DONE);
        }
    }

    private PasswordFragment mPasswordFragment;
    private String mPassword;

    public static final MainFragment newInstance(String type) {
        MainFragment fragment = new MainFragment();
        Bundle bundle = new Bundle();
        bundle.putString("KEY_MODE", type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMode = getArguments().getString("KEY_MODE");
    }

    private Button.OnClickListener mClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mMode.equals("Encrypt")) {
                mListView.setVisibility(View.VISIBLE);
                mEncryptButton.setVisibility(View.GONE);
                listFiles(new File("/sdcard/"));
            } else if (mMode.equals("Decrypt")) {
                mListView.setVisibility(View.VISIBLE);
                mDecryptButton.setVisibility(View.GONE);
                listFiles(new File("/sdcard/SecretLocker"));
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_main, container, false);
        mEncryptButton = (Button) mView.findViewById(R.id.btn_encrypt);
        mEncryptButton.setOnClickListener(mClick);
        mDecryptButton = (Button) mView.findViewById(R.id.btn_decrypt);
        mDecryptButton.setOnClickListener(mClick);

        mListView = (ListView) mView.findViewById(R.id.files_list);
        mListView.setOnItemClickListener(onItemClick);

        if (mMode.equals("Encrypt")) {
            mEncryptButton.setVisibility(View.VISIBLE);
        } else if (mMode.equals("Decrypt")) {
            mDecryptButton.setVisibility(View.VISIBLE);
        }

        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle("");
        mProgressDialog.setMessage(getString(R.string.progress_dialog_text));
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mPasswordFragment = new PasswordFragment();
        mPasswordFragment.setTargetFragment(this, 0);

        return mView;
    }

    private void encrypt() {
        try {
            mFileInputStream = new FileInputStream(mFile);

            File encryptFile = new File(LockerMain.getFolder(), getFileName() + "~lock");
            if (!encryptFile.exists()) {
                encryptFile.createNewFile();
            }

            mFileOutputStream = new FileOutputStream(encryptFile);

            mCipher = mCipher.getInstance("AES/CBC/PKCS5Padding");
            mIvParameterSpec = new IvParameterSpec(decideKeyLength(mPassword).getBytes());
            mSecretKeySpec = new SecretKeySpec(decideKeyLength(mPassword).getBytes(), "AES");
            mCipher.init(Cipher.ENCRYPT_MODE, mSecretKeySpec, mIvParameterSpec);

            mCipherOutputStream = new CipherOutputStream(mFileOutputStream, mCipher);

            int data_byte;
            byte[] data_bytes_block = new byte[8];
            while ((data_byte = mFileInputStream.read(data_bytes_block)) != -1) {
                mCipherOutputStream.write(data_bytes_block, 0, data_byte);
            }

            mCipherOutputStream.flush();
            mCipherOutputStream.close();
            mFileInputStream.close();
            mFileOutputStream.close();

            mFile.delete();

        } catch (Exception e) {
            Log.d("Wayne", e.toString());
        }
    }

    private void decrypt() {
        try {
            mFileInputStream = new FileInputStream(mFile);

            File decryptFile = new File(LockerMain.getFolder(), getOriginalFileNameWithExtension());

            mCipher = mCipher.getInstance("AES/CBC/PKCS5Padding");
            mIvParameterSpec = new IvParameterSpec(decideKeyLength(mPassword).getBytes());
            mSecretKeySpec = new SecretKeySpec(decideKeyLength(mPassword).getBytes(), "AES");
            mCipher.init(Cipher.DECRYPT_MODE, mSecretKeySpec, mIvParameterSpec);

            mFileOutputStream = new FileOutputStream(decryptFile);
            mCipherInputStream = new CipherInputStream(mFileInputStream, mCipher);

            int data_byte;
            byte[] data_bytes_block = new byte[8];
            while ((data_byte = mCipherInputStream.read(data_bytes_block)) != -1) {
                mFileOutputStream.write(data_bytes_block, 0, data_byte);
            }
            mFileOutputStream.flush();
            mCipherInputStream.close();
            mFileInputStream.close();
            mFileOutputStream.close();

            mFile.delete();

        } catch (Exception e) {
            Log.d("Wayne", e.toString());
        }
    }

    private String getFileName() {
        String[] path = mFile.getPath().split("/");
        String nameWithExtension = path[path.length - 1];
        return nameWithExtension;
    }

    private String getOriginalFileNameWithExtension() {
        String[] path = mFile.getPath().split("/");
        String nameWithExtension = path[path.length - 1];
        String[] originalName = nameWithExtension.split("~");
        return originalName[0];
    }

    private String decideKeyLength(String key) {
        int length = key.length();

        if (length < 16) { //Generate 16 bits key
            for (int i = length; i < 16; ++i)
                key += i % 10;
            return key;
        } else if (length < 24) { //Generate 24 bits key
            for (int i = length; i < 24; ++i)
                key += i % 10;
            return key;
        } else if (length < 32) { //Generate 32 bits key
            for (int i = length; i < 32; ++i)
                key += i % 10;
            return key;
        }
        return key.substring(0, 32);
    }

    private void listFiles(File file) {
        mSubFiles = file.listFiles();
        mFilesArrayList = new ArrayList<>();

        for (File f : mSubFiles) {
            HashMap<String, String> item = new HashMap<>();
            if (f.isDirectory()) {
                item.put("Path", f.getAbsolutePath());
                item.put("Type", getString(R.string.file_type_directory));
                mFilesArrayList.add(item);
            } else {
                item.put("Path", f.getAbsolutePath());
                item.put("Type", getString(R.string.file_type_file));
                mFilesArrayList.add(item);
            }
        }

        Collections.sort(mFilesArrayList, new Comparator<HashMap<String, String>>() {
            @Override
            public int compare(HashMap<String, String> lhs, HashMap<String, String> rhs) {
                String firstValue = lhs.get("Path");
                String secondValue = rhs.get("Path");
                return firstValue.compareTo(secondValue);
            }
        });

        mSimpleAdapter = new SimpleAdapter(getActivity(), mFilesArrayList, android.R.layout.simple_list_item_2,
                new String[]{"Path", "Type"}, new int[]{android.R.id.text1, android.R.id.text2});
        mListView.setAdapter(mSimpleAdapter);
    }

    @Override
    public void OnPositiveClick() {
        EditText text = (EditText) mPasswordFragment.getDialog().findViewById(R.id.edit_text_pwd);
        mPassword = text.getText().toString();
        if (mPassword.length() != 0) {
            doWork();
        }
    }

    @Override
    public void OnNegativeClick() {
    }

    private void doWork() {
        if (mMode.equals("Encrypt")) {
            mEncryptButton.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mEncryptButton.setEnabled(false);
            mDecryptButton.setEnabled(false);

            mProgressDialog.show();
            mEncryptThread = new EncryptThread();
            mEncryptThread.start();

        } else if (mMode.equals("Decrypt")) {
            mDecryptButton.setVisibility(View.VISIBLE);
            mListView.setVisibility(View.GONE);
            mEncryptButton.setEnabled(false);
            mDecryptButton.setEnabled(false);

            mProgressDialog.show();
            mDecryptThread = new DecryptThread();
            mDecryptThread.start();
        }
    }
}
