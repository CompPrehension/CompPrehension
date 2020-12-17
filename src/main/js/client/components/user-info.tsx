import { observer } from 'mobx-react';
import * as React from 'react';
import store from '../store';


export const UserInfo = observer(() => {
    const { sessionInfo } = store;
    if (!sessionInfo) {
        return null;
    }

    return (
        <div>
            <div style={{float: 'right'}}>
                Вы вошли под именем {sessionInfo.user.displayName}
            </div>
        </div>
    );
});
