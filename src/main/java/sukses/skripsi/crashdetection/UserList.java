package sukses.skripsi.crashdetection;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class UserList extends ArrayAdapter<Users> {
    private Activity context;
    private List<Users> usersList;
    List<String> handphone = new ArrayList<>();

    public  UserList(Activity context, List<Users> usersList){
        super(context, R.layout.list_layout,usersList);
        this.context = context;
        this.usersList = usersList;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View listViewItem = inflater.inflate(R.layout.list_layout, null, true);

        TextView txvName =listViewItem.findViewById(R.id.nameList);
        TextView txvPhoneNumber =listViewItem.findViewById(R.id.phoneNumberList);
        Users users =usersList.get(position);

        txvName.setText(users.getName());
        txvPhoneNumber.setText(users.getNoHp());

        final String name = users.getName();
        final String phone = users.getNoHp();

        txvName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handphone.add(phone);
            }
        });

        return listViewItem;
    }

    public List<String> getHandphone() {
        return handphone;
    }

    public void setHandphone(List<String> handphone) {
        this.handphone = handphone;
    }
}
