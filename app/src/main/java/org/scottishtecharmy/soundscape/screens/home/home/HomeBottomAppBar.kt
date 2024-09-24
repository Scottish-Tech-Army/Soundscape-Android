package org.scottishtecharmy.soundscape.screens.home.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import org.scottishtecharmy.soundscape.R
import org.scottishtecharmy.soundscape.viewmodels.AheadOfMeViewModel
import org.scottishtecharmy.soundscape.viewmodels.MyLocationViewModel
import org.scottishtecharmy.soundscape.viewmodels.WhatsAroundMeViewModel

@Composable
fun HomeBottomAppBar(useView: Boolean = true) {
    val context = LocalContext.current
    val notAvailableText = "This is not implemented yet."
    val notAvailableToast = {
        Toast.makeText(context, notAvailableText, Toast.LENGTH_SHORT).show()
    }

    var myLocationViewModel: MyLocationViewModel? = null
    var whatsAroundMeViewModel: WhatsAroundMeViewModel? = null
    var aheadOfMeViewModel: AheadOfMeViewModel? = null
    if (useView)
        {
            myLocationViewModel = hiltViewModel<MyLocationViewModel>()
            whatsAroundMeViewModel = hiltViewModel<WhatsAroundMeViewModel>()
            aheadOfMeViewModel = hiltViewModel<AheadOfMeViewModel>()
        }

    BottomAppBar(
        modifier =
        Modifier
            .height(150.dp)
            .clip(RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                Text(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    text = stringResource(R.string.callouts_panel_title).uppercase(),
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier =
                Modifier
                    .fillMaxWidth(),
            ) {
                Button(
                    onClick = {
                        myLocationViewModel?.myLocation()
                    },
                    shape = RectangleShape,
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.my_location_24px),
                            contentDescription = stringResource(R.string.user_activity_my_location_title),
                            modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_my_location),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Button(
                    onClick = { whatsAroundMeViewModel?.whatsAroundMe() },
                    shape = RectangleShape,
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.around_me_24px),
                            contentDescription = stringResource(R.string.user_activity_around_me_title),
                            modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_around_me),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Button(
                    onClick = { aheadOfMeViewModel?.aheadOfMe() },
                    shape = RectangleShape,
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.ahead_of_me_24px),
                            contentDescription = stringResource(R.string.user_activity_ahead_of_me_title),
                            modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_ahead_of_me),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Button(
                    onClick = { notAvailableToast() },
                    shape = RectangleShape,
                ) {
                    Column {
                        Icon(
                            painter = painterResource(R.drawable.nearby_markers_24px),
                            contentDescription = stringResource(R.string.user_activity_nearby_markers_title),
                            modifier =
                            Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally),
                        )
                        Text(
                            softWrap = true,
                            text = stringResource(R.string.ui_action_button_nearby_markers),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}