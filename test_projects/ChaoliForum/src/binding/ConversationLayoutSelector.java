package com.daquexian.chaoli.forum.binding;

import com.daquexian.chaoli.forum.R;
import com.daquexian.chaoli.forum.model.Conversation;

/**
 * Created by daquexian on 16-11-10.
 */

public class ConversationLayoutSelector extends LayoutSelector<Conversation> {
    @Override
    int getLayout(int type) {
        switch (type) {
            case 0:
                return R.layout.conversation_view;
            case FOOTER_VIEW:
                return R.layout.loading_item_for_conversation;
        }
        throw new IllegalArgumentException("wrong type");
    }

    @Override
    int getType(Conversation item) {
        if (item.getReplies() == -1) return FOOTER_VIEW;
        return 0;
    }
}
