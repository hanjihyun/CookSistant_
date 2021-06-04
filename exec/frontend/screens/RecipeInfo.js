import React, { Component } from 'react';
import {
  StyleSheet,
  Dimensions,
  ScrollView,
  Image,
  ImageBackground,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { Block, Text, theme, Button as GaButton } from 'galio-framework';
import { Button } from '../components';
import { Images, nowTheme } from '../constants';
import axios from 'axios';
import firebase from 'firebase';

const { width, height } = Dimensions.get('screen');

class RecipeInfo extends Component {
  state = {
    userId: null,

    img: require('../assets/imgs/bookmark.png'),
    id: this.props.route.params.id,

    recipeDetail: {
      id: 0,
      nickname: null,
      cuisine: null,
      description: null,
      cookingTime: null,
      image: null,
      level: null,
      serving: null,
      ingredientDTOList: [],
      stepList: [],
    },
  };

  componentDidMount = () => {
    // 로그인된 회원
    var user = firebase.auth().currentUser;

    if (user) {
      axios
        .get(`http://j4c101.p.ssafy.io:8081/user/${user.uid}`)
        .then((result) => {
          // console.log(result.data.userId);
          this.setState({ userId: result.data.userId });
          // 로그인된 회원이 스크랩한 레시피 인가?
          axios
            .get(`http://j4c101.p.ssafy.io:8081/user/isscrap/${this.state.userId}/${this.state.id}`)
            .then((result) => {
              if (result.data == true) {
                this.setState({ img: require('../assets/imgs/bookmarkFull.png') });
              }
            })
            .catch((error) => {
              console.log(error);
            });
        })
        .catch((error) => {
          console.log(error);
        });
    }

    axios
      .get(`http://j4c101.p.ssafy.io:8081/recipe/show/${this.state.id}`)
      .then((result) => {
        const IngredientList = [];
        const stepList = [];
        console.log(result);
        result.data.ingredientDTOList.forEach((el) => {
          const tmp = el.amount.split('(');
          const tmp1 = el.ingredientName.split('(');

          IngredientList.push({
            ingredientName: tmp1[0],
            amount: tmp[0],
            isType: el.isType,
          });
        }),
          result.data.stepList.forEach((el) => {
            stepList.push({
              description: el.description,
              image: el.image,
              level: el.level,
            });
          }),
          this.setState({
            recipeDetail: {
              id: result.data.recipeId,
              nickname: result.data.nickname,
              cuisine: result.data.cuisine,
              description:
                result.data.description == 'None' ? '소개글이 없습니다.' : result.data.description,
              cookingTime: result.data.cookingTime,
              image: result.data.image,
              level: result.data.level,
              serving:
                result.data.serving.length > 0 ? result.data.serving + ' |' : result.data.serving,
              ingredientDTOList: IngredientList,
              stepList: stepList,
            },
          });
      })
      .catch((error) => {
        console.log(error);
      });
  };

  changeImage = () => {
    if (this.state.userId == null) {
      Alert.alert('로그인 후 이용해주세요');
    } else {
      axios
        .get(`http://j4c101.p.ssafy.io:8081/user/isscrap/${this.state.userId}/${this.state.id}`)
        .then((result) => {
          // console.log('result_____________________________' + result.data);
          if (result.data == true) {
            axios
              .put(
                `http://j4c101.p.ssafy.io:8081/user/deleteScrap/${this.state.userId}/${this.state.recipeDetail.id}`
              )
              .then((result) => this.setState({ img: require('../assets/imgs/bookmark.png') }))
              .catch((error) => {
                console.log(error);
              });
          } else if (result.data == false) {
            axios
              .post(
                `http://j4c101.p.ssafy.io:8081/user/scrap/${this.state.recipeDetail.id}/${this.state.userId}`
              )
              .then((result) => this.setState({ img: require('../assets/imgs/bookmarkFull.png') }))

              .catch((error) => {
                console.log(error);
              });
          }
        })
        .catch((error) => {
          console.log(error);
        });
    }
  };

  renderDetail = () => {
    return (
      <Block style={styles.container}>
        <Block>
          <ImageBackground
            source={Images.RegisterBackground}
            style={styles.imageBackgroundContainer}
            imageStyle={styles.imageBackground}
          >
            <Block flex={1} center>
              <Block center style={styles.infoContainer}>
                <Block>
                  <ScrollView showsVerticalScrollIndicator={false}>
                    <Block>
                      <Image
                        resizeMode="cover"
                        source={{ uri: this.state.recipeDetail.image }}
                        style={{
                          width: width * 0.9,
                          height: 200,
                        }}
                      />
                    </Block>
                    <Block row left style={{ marginTop: 20, marginLeft: 15, marginBottom: 10 }}>
                      <Block style={{ width: width * 0.69 }}>
                        <Text
                          style={{
                            fontFamily: 'montserrat-bold',
                            fontSize: 15,
                            lineHeight: 18,
                          }}
                          color="#474747"
                        >
                          {this.state.recipeDetail.cuisine}
                        </Text>
                      </Block>
                      <TouchableOpacity activeOpacity={0.5} onPress={this.changeImage}>
                        <Image source={this.state.img} style={{ marginLeft: 15 }} />
                      </TouchableOpacity>
                    </Block>

                    <Block row style={{ marginLeft: 15, marginBottom: 4 }}>
                      <Text color="#474747" size={13} style={{ fontFamily: 'montserrat-regular' }}>
                        {this.state.recipeDetail.serving}
                      </Text>
                      <Text size={13}> </Text>
                      <Text color="#474747" size={13} style={{ fontFamily: 'montserrat-regular' }}>
                        {this.state.recipeDetail.cookingTime} 
                      </Text>
                    </Block>
                    <Block
                      center
                      style={{ width: width * 0.9, alignItems: 'center', marginTop: 5 }}
                    >
                      <Text
                        size={16}
                        muted
                        style={{
                          textAlign: 'center',
                          fontFamily: 'montserrat-regular',
                          color: '#2c2c2c',
                          lineHeight: 20,
                          fontSize: 11,
                          padding: 0,
                        }}
                      >
                        {this.state.recipeDetail.description} 
                      </Text>
                    </Block>
                    <Block
                      style={{
                        borderColor: '#b6b7b7',
                        width: '90%',
                        borderWidth: StyleSheet.hairlineWidth,
                        marginHorizontal: 10,
                        marginTop: 15,
                      }}
                    />
                    <Block style={{ width: width * 0.9 }}>
                      <Text style={styles.titleStyle}> 재료</Text>

                      <Block row style={{ marginLeft: 10, flexWrap: 'wrap' }}>
                        {this.state.recipeDetail.ingredientDTOList.map((idx, index) => (
                          <Block key={index} style={styles.ingreBtn}>
                            <Block row>
                              <Text
                                style={{
                                  fontSize: Platform.OS === 'android' ? 11 : 13,
                                  color: 'white',
                                  margin: 5,
                                  fontFamily: 'montserrat-bold',
                                }}
                              >
                                {idx.ingredientName}
                              </Text>
                            </Block>
                            <Block
                              row
                              style={{
                                width: idx.amount.length == 0 ? 0 : 55,
                                height: idx.amount.length == 0 ? 0 : 25,
                                borderRadius: 20,
                                borderColor: '#F18D46',
                                backgroundColor: 'white',
                                borderWidth: 1,
                                overflow: 'hidden',
                                padding: 5,
                                justifyContent: 'center',
                              }}
                            >
                              <Text
                                style={{
                                  fontSize: 10,
                                  fontFamily: 'montserrat-bold',
                                  textAlign: 'center',
                                }}
                              >
                                {idx.amount}
                              </Text>
                            </Block>
                          </Block>
                        ))}
                      </Block>
                    </Block>
                  </ScrollView>
                </Block>
              </Block>
            </Block>
          </ImageBackground>
        </Block>
      </Block>
    );
  };

  render() {
    return (
      <Block style={styles.mainViewStyle}>
        <Block flex={9}>{this.renderDetail()}</Block>
        <Block flex={1} style={styles.underMenu}>
          <Button
            style={styles.btnStyle}
            textStyle={{ fontSize: 15, color: '#F18D46', fontFamily: 'montserrat-bold' }}
            color="Primary"
            round
            onPress={() =>
              this.state.userId == null
                ? Alert.alert('로그인 후 이용해주세요')
                : this.props.navigation.navigate('TTS', { step: this.state.recipeDetail })
            }
          >
            요리시작
          </Button>
        </Block>
      </Block>
    );
  }
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    flexDirection: 'column',
  },
  imageBackgroundContainer: {
    width: width,
    height: height,
    padding: 0,
  },
  imageBackground: {
    width: width,
    height: height,
  },
  infoContainer: {
    marginTop: height > 800 ? 5 : 10,
    width: width * 0.9,
    height: height > 800 ? height * 0.78 : height * 0.725,
    backgroundColor: nowTheme.COLORS.WHITE,
    borderRadius: 4,
    shadowColor: nowTheme.COLORS.BLACK,
    shadowOffset: {
      width: 0,
      height: 4,
    },
    shadowRadius: 8,
    shadowOpacity: 0.1,
    elevation: 1,
    overflow: 'hidden',
  },
  mainViewStyle: { flexGrow: 1, marginTop: height > 800 ? 100 : 60 },

  ingreBtn: {
    width: width * 0.4,
    height: 52,
    margin: 5,
    backgroundColor: '#F18D46',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 30,
  },
  ingreTxt: {
    color: 'white',
    margin: 5,
    fontFamily: 'montserrat-bold',
  },
  amoutTxt: {
    fontSize: 10,
    fontFamily: 'montserrat-bold',
    textAlign: 'center',
  },
  titleStyle: {
    color: '#2c2c2c',
    fontWeight: 'bold',
    fontSize: 19,
    fontFamily: 'montserrat-bold',
    margin: 15,
    marginLeft: 20,
  },
  underMenu: {
    alignItems: 'center',
    marginBottom: 10,
    bottom: -10,
    backgroundColor: 'white',
  },
  btnStyle: {
    width: 200,
    height: 44,
    marginHorizontal: 10,
    elevation: 0,
    alignItems: 'center',
    backgroundColor: 'white',
    borderWidth: 4,
    borderColor: '#F18D46',
  },
});
export default RecipeInfo;
