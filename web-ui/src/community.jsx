var ReactDOM = require('react-dom'),
    React = require('react'),
    Bootstrap = require('react-bootstrap'),
    Footer = require('./component/footer.jsx'),
    Navigator = require('./component/navigator.jsx'),
    FriendLink = require('./component/friend-link.jsx'),
    Badge = Bootstrap.Badge,
    Label = Bootstrap.Label,
    Pagination = Bootstrap.Pagination,
    Row = Bootstrap.Row,
    Grid = Bootstrap.Grid,
    Col = Bootstrap.Col,
    Media = Bootstrap.Media,
    ListGroup = Bootstrap.ListGroup,
    ListGroupItem = Bootstrap.ListGroupItem,
    Nav = Bootstrap.Nav,
    NavItem = Bootstrap.NavItem;

const CommunityList = React.createClass({
    getDefaultProps: function () {
        return {
            tab: 1, page: 1, nav: [
                {tab: '最新更新', category: 'all'},
                {tab: '问答板', category: 'question'},
                {tab: '入门者说', category: 'beginner'},
                {tab: '技术分享', category: 'share'},
                {tab: '精品区', category: 'awesome'}
            ]
        }
    },
    getInitialState: function () {
        return {
            tab: this.props.tab,
            page: this.props.page,
            articles: [{
                "create_time": "2 天 前",
                "avatar_url": "https://avatars.githubusercontent.com/u/7821898?v=3",
                "author": 7821898,
                "html_url": "https://github.com/chpengzh",
                "name": "手不要乱摸",
                "comment": 0,
                "title": "我是来测试发送的4",
                "category": "精华",
                "aid": 4,
                "email": "chpengzh@foxmail.com",
                "flower": 0
            }, {
                "create_time": "2 天 前",
                "avatar_url": "https://avatars.githubusercontent.com/u/7821898?v=3",
                "author": 7821898,
                "html_url": "https://github.com/chpengzh",
                "name": "手不要乱摸",
                "comment": 0,
                "title": "我是来测试发送的3",
                "category": "求助",
                "aid": 3,
                "email": "chpengzh@foxmail.com",
                "flower": 1
            }, {
                "create_time": "2 天 前",
                "avatar_url": "https://avatars.githubusercontent.com/u/7821898?v=3",
                "author": 7821898,
                "html_url": "https://github.com/chpengzh",
                "name": "手不要乱摸",
                "comment": 2,
                "title": "我是来测试发送的2",
                "category": "技术分享",
                "aid": 2,
                "email": "chpengzh@foxmail.com",
                "flower": 0
            }, {
                "create_time": "2 天 前",
                "avatar_url": "https://avatars.githubusercontent.com/u/7821898?v=3",
                "author": 7821898,
                "html_url": "https://github.com/chpengzh",
                "name": "手不要乱摸",
                "comment": 0,
                "title": "我是来测试发送的",
                "category": "入门者说",
                "aid": 1,
                "email": "chpengzh@foxmail.com",
                "flower": 0
            }],
            nav: this.props.nav,
            loading: false
        }
    },
    render: function () {
        return <Grid><Row>
            <Col sm={12} md={9}>
                <Nav bsStyle="tabs" activeKey={this.state.tab} style={{marginBottom:5}}>{this.getNavList()}</Nav>
                <ListGroup>{this.getArticleView()}</ListGroup>
                <div className="text-center">
                    <Pagination prev next first last ellipsis boundaryLinks className="text-center"
                                items={30} maxButtons={5} activePage={this.state.page}
                                onSelect={this.handleSelectPage}/>
                </div>
            </Col>
            <Col sm={6} md={3}><FriendLink/></Col>
        </Row></Grid>
    },
    handleSelectPage: function (page) {
        this.setState({page: page});
    },
    getNavList: function () {
        var view = [];
        for (var i = 1; i <= this.state.nav.length; i++) {
            const index = i;
            view.push(
                <NavItem className={(this.state.tab == index) ? 'active' : ''}
                         onClick={() => this.setState({tab: index})}>
                    {this.state.nav[index - 1].tab}
                </NavItem>
            );
        }
        return view
    },
    getArticleView: function () {
        var view = [];
        for (var i = 0; i < this.state.articles.length; i++) {
            const article = this.state.articles[i];
            view.push(
                <ListGroupItem href={article.html_url} ref={'article-id-' + article.aid}>
                    <Media><Media.Left align="top">
                        <img width={64} height={64} src={article.avatar_url} alt="Avatar"/>
                    </Media.Left><Media.Body>
                        <Media.Heading style={{'marginTop': '10px'}}>
                            {article.title + '  '}
                            <small>{article.category == 'default' ? '' : article.category}</small>
                        </Media.Heading>
                        <small>
                            <Label bsStyle="success">{article.create_time}</Label>
                            {' • ' + article.author + ' • 赞'}
                            <Badge style={{padding: '1px 7px'}}>{article.flower}</Badge>
                            {' • 评论 '}
                            <Badge style={{padding: '1px 7px'}}>{article.comment}</Badge>
                        </small>
                    </Media.Body></Media>
                </ListGroupItem>
            );
        }
        return view;
    }
});

ReactDOM.render((
    <div>
        <Navigator/>
        <CommunityList/>
        <Footer/>
    </div>
), document.body);
